package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.history.GitRef
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Reads the commit history of [projectRoot] with `git log` over branches, remotes, tags and HEAD. Not `--all`: that
 * also walks private refs other tools keep (cmux's last-turn snapshots under `refs/cmux`, stashes), and each one draws a stray
 * side lane of snapshot and index commits through the graph. The HEAD of every other worktree is walked too, so work
 * going on in a detached checkout shows, and its commit carries a [GitRef.Kind.Worktree] label. Output is read while the
 * process runs: a long history overflows the pipe buffer, and waiting for exit first
 * would hang. A repository with no commits yet is an empty [GitHistoryResult.Loaded].
 */
internal object WorkbenchGitLogScanner {
    fun scan(projectRoot: File, maxCount: Int = DefaultMaxCount): GitHistoryResult {
        if (!projectRoot.isDirectory) return GitHistoryResult.Unavailable("Project folder not found.")

        val worktrees = WorkbenchGitWorktreeHeads.scan(projectRoot)
        val process = try {
            ProcessBuilder(
                listOf(
                    "git", "-C", projectRoot.absolutePath, "log",
                    "--branches", "--remotes", "--tags", "HEAD",
                ) + worktrees.map { head -> head.hash }.distinct() + listOf(
                    "--topo-order", "--decorate=full",
                    "--max-count=$maxCount", "--pretty=$GIT_LOG_PRETTY_FORMAT",
                ),
            ).also { builder -> builder.environment()["LC_ALL"] = "C" }.start()
        } catch (error: Exception) {
            return GitHistoryResult.Unavailable("Could not run git: ${error.message ?: "unknown error"}")
        }

        var stdout = ""
        var stderr = ""
        val outReader = thread(isDaemon = true, name = "moru-git-log-out") {
            stdout = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        }
        val errReader = thread(isDaemon = true, name = "moru-git-log-err") {
            stderr = process.errorStream.bufferedReader(Charsets.UTF_8).readText()
        }

        if (!process.waitFor(TimeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return GitHistoryResult.Unavailable("git log did not finish in time.")
        }
        outReader.join(ReaderJoinMillis)
        errReader.join(ReaderJoinMillis)

        if (process.exitValue() != 0) {
            return when {
                EmptyRepositoryMarker in stderr || UnbornHeadMarker in stderr ->
                    GitHistoryResult.Loaded(emptyList(), truncated = false)
                else -> GitHistoryResult.Unavailable(
                    stderr.lineSequence().firstOrNull { line -> line.isNotBlank() }
                        ?: "Not a Git repository.",
                )
            }
        }

        val worktreeRefs = worktrees.groupBy({ head -> head.hash }, { head -> head.ref })
        val commits = parseGitLog(stdout).map { commit ->
            worktreeRefs[commit.hash]?.let { refs -> commit.copy(refs = commit.refs + refs) } ?: commit
        }
        return GitHistoryResult.Loaded(commits, truncated = commits.size >= maxCount)
    }

    private const val DefaultMaxCount = 300
    private const val TimeoutSeconds = 10L
    private const val ReaderJoinMillis = 2_000L

    // git prints this (in the C locale) when HEAD points at a branch with no commits yet.
    private const val EmptyRepositoryMarker = "does not have any commits yet"

    // ...and this when HEAD is named as a revision before its branch has a commit.
    private const val UnbornHeadMarker = "ambiguous argument 'HEAD'"
}
