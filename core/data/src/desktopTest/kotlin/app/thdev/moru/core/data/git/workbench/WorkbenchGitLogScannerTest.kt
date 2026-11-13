package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.history.GitRef
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Runs the scanner against real git repositories, so the parser meets real output. */
class WorkbenchGitLogScannerTest {
    private fun withRepo(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("moru-git-log").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun git(dir: File, vararg args: String) {
        val process = ProcessBuilder(listOf("git", "-C", dir.absolutePath) + args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) { "git ${args.toList()} failed: $output" }
    }

    private fun initRepo(dir: File) {
        git(dir, "init", "-q", "-b", "main")
        git(dir, "config", "user.name", "Test Author")
        git(dir, "config", "user.email", "test@example.com")
        git(dir, "config", "commit.gpgsign", "false")
    }

    private fun commitFile(dir: File, name: String, message: String) {
        File(dir, name).writeText("$name\n")
        git(dir, "add", name)
        git(dir, "commit", "-q", "-m", message)
    }

    @Test
    fun scansARepositoryWithABranchAndAMergeCommit() = withRepo { dir ->
        initRepo(dir)
        commitFile(dir, "a.txt", "first commit")
        git(dir, "checkout", "-q", "-b", "feat")
        commitFile(dir, "b.txt", "feature work")
        git(dir, "checkout", "-q", "main")
        commitFile(dir, "c.txt", "main work")
        git(dir, "merge", "-q", "--no-ff", "feat", "-m", "Merge feat into main")
        git(dir, "tag", "v1")

        val result = assertIs<GitHistoryResult.Loaded>(WorkbenchGitLogScanner.scan(dir))

        assertEquals(4, result.commits.size)
        assertEquals(false, result.truncated)
        val merge = result.commits.first()
        assertEquals("Merge feat into main", merge.subject)
        assertEquals(2, merge.parents.size, "a --no-ff merge has two parents")
        assertEquals("Test Author", merge.authorName)
        assertTrue(GitRef("main", GitRef.Kind.LocalBranch, isCurrent = true) in merge.refs)
        assertTrue(GitRef("v1", GitRef.Kind.Tag) in merge.refs)
        assertTrue(
            result.commits.any { commit -> GitRef("feat", GitRef.Kind.LocalBranch) in commit.refs },
            "the feature branch tip must carry its label",
        )
    }

    @Test
    fun stashesAndOtherToolsPrivateRefsStayOutOfTheGraph() = withRepo { dir ->
        initRepo(dir)
        commitFile(dir, "a.txt", "first commit")
        File(dir, "a.txt").writeText("changed\n")
        git(dir, "stash", "-q")
        git(dir, "update-ref", "refs/cmux/last-turn/abc", "stash@{0}")
        git(dir, "checkout", "-q", "--detach")
        commitFile(dir, "b.txt", "detached work")

        val result = assertIs<GitHistoryResult.Loaded>(WorkbenchGitLogScanner.scan(dir))

        assertEquals(listOf("detached work", "first commit"), result.commits.map { commit -> commit.subject })
    }

    @Test
    fun anotherWorktreesDetachedWorkShowsWithItsFolderAndPendingChanges() = withRepo { dir ->
        val repo = File(dir, "repo").apply { mkdirs() }
        initRepo(repo)
        commitFile(repo, "a.txt", "first commit")
        val other = File(dir, "agent-task")
        git(repo, "worktree", "add", "-q", "--detach", other.absolutePath)
        commitFile(other, "b.txt", "agent work")
        File(other, "b.txt").writeText("still editing\n")
        File(other, "new.txt").writeText("draft\n")

        val result = assertIs<GitHistoryResult.Loaded>(WorkbenchGitLogScanner.scan(repo))

        assertEquals(listOf("agent work", "first commit"), result.commits.map { commit -> commit.subject })
        assertEquals(listOf(GitRef("agent-task", GitRef.Kind.Worktree, pendingChanges = 2)), result.commits.first().refs)
        assertTrue(result.commits.none { commit -> commit.refs.any { ref -> ref.name == "repo" } },
            "the checkout being shown is not labelled as another worktree")
    }

    @Test
    fun maxCountMarksTheHistoryAsTruncated() = withRepo { dir ->
        initRepo(dir)
        repeat(5) { index -> commitFile(dir, "f$index.txt", "commit $index") }

        val result = assertIs<GitHistoryResult.Loaded>(WorkbenchGitLogScanner.scan(dir, maxCount = 3))

        assertEquals(3, result.commits.size)
        assertEquals(true, result.truncated)
    }

    @Test
    fun aRepositoryWithoutCommitsIsEmptyNotAnError() = withRepo { dir ->
        initRepo(dir)

        val result = assertIs<GitHistoryResult.Loaded>(WorkbenchGitLogScanner.scan(dir))

        assertEquals(emptyList(), result.commits)
    }

    @Test
    fun aFolderThatIsNotARepositoryIsUnavailableWithAMessage() = withRepo { dir ->
        val result = assertIs<GitHistoryResult.Unavailable>(WorkbenchGitLogScanner.scan(dir))

        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun aMissingFolderIsUnavailable() {
        val result = WorkbenchGitLogScanner.scan(File("/definitely/not/here-${System.nanoTime()}"))

        assertIs<GitHistoryResult.Unavailable>(result)
    }

    @Test
    fun aLongHistoryDoesNotHangOnTheOutputPipe() = withRepo { dir ->
        initRepo(dir)
        // Long subjects on purpose: 160 of them make about 130 KB of output, past a 64 KB pipe buffer.
        repeat(160) { index ->
            git(dir, "commit", "--allow-empty", "-q", "-m", "commit $index " + "x".repeat(600))
        }

        val result = assertIs<GitHistoryResult.Loaded>(WorkbenchGitLogScanner.scan(dir, maxCount = 200))

        assertEquals(160, result.commits.size)
    }
}
