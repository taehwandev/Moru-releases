package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import java.io.File

/**
 * Reads changes from git as diffs: what one commit changed, or what is changed but not committed yet.
 * Output is capped and reported as truncated, so a huge change cannot freeze the screen.
 */
internal object WorkbenchGitDiffScanner {
    /** What one commit changed. A merge is shown against its first parent, the way a branch history reads. */
    fun commit(projectRoot: File, hash: String): CommitDiffResult {
        if (!projectRoot.isDirectory) return CommitDiffResult.Unavailable("Project folder not found.")
        // The hash goes to git as an argument, so anything but hex could be read as an option.
        if (!HexHash.matches(hash)) return CommitDiffResult.Unavailable("The commit hash is not valid.")

        val run = WorkbenchGitProcess.run(
            projectRoot,
            "show", "--format=", "--patch", "--find-renames", "--no-color", "--no-ext-diff",
            "-m", "--first-parent", hash,
        ) ?: return CommitDiffResult.Unavailable("Could not run git.")
        if (run.timedOut) return CommitDiffResult.Unavailable("git show did not finish in time.")
        if (!run.truncated && run.exitCode != 0) {
            return CommitDiffResult.Unavailable(run.firstErrorLine() ?: "Could not read the changes.")
        }
        return CommitDiffResult.Loaded(parseGitDiff(run.stdout), run.truncated)
    }

    /**
     * Everything changed since the last commit: edits to tracked files, staged or not, plus new files
     * git does not track yet, which an agent creates all the time. A repository with no commits yet
     * shows its files as new.
     */
    fun workingTree(projectRoot: File): CommitDiffResult {
        if (!projectRoot.isDirectory) return CommitDiffResult.Unavailable("Project folder not found.")

        val hasHead = WorkbenchGitProcess.run(projectRoot, "rev-parse", "--verify", "--quiet", "HEAD")?.exitCode == 0
        val tracked = if (hasHead) {
            WorkbenchGitProcess.run(projectRoot, "diff", "HEAD", "--patch", "--find-renames", "--no-color", "--no-ext-diff")
        } else {
            WorkbenchGitProcess.run(projectRoot, "diff", "--patch", "--no-color", "--no-ext-diff")
        } ?: return CommitDiffResult.Unavailable("Could not run git.")
        if (tracked.timedOut) return CommitDiffResult.Unavailable("git diff did not finish in time.")
        if (tracked.exitCode != 0) {
            return CommitDiffResult.Unavailable(tracked.firstErrorLine() ?: "Not a Git repository.")
        }

        val files = parseGitDiff(tracked.stdout).toMutableList()
        var truncated = tracked.truncated
        val untracked = untrackedFiles(projectRoot)
        for (path in untracked.take(MaxUntrackedFiles)) {
            files += newFileDiff(projectRoot, path) ?: continue
        }
        if (untracked.size > MaxUntrackedFiles) truncated = true
        return CommitDiffResult.Loaded(files, truncated)
    }

    private fun untrackedFiles(projectRoot: File): List<String> {
        val run = WorkbenchGitProcess.run(projectRoot, "ls-files", "--others", "--exclude-standard", "-z") ?: return emptyList()
        if (run.exitCode != 0) return emptyList()
        return run.stdout.split('\u0000').filter { path -> path.isNotEmpty() }
    }

    /** `--no-index` against /dev/null is how git itself prints a new file; it exits 1 when files differ. */
    private fun newFileDiff(projectRoot: File, path: String): List<DiffFile>? {
        val run = WorkbenchGitProcess.run(
            projectRoot,
            "diff", "--no-index", "--no-color", "--no-ext-diff", "--", "/dev/null", path,
        ) ?: return null
        if (run.exitCode > 1) return null
        return parseGitDiff(run.stdout)
    }

    private val HexHash = Regex("^[0-9a-fA-F]{7,64}$")
    private const val MaxUntrackedFiles = 100
}
