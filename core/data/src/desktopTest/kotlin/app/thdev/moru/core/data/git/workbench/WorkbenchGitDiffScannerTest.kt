package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Runs the scanner against real git repositories, so the parser meets real `git show` output. */
class WorkbenchGitDiffScannerTest {
    private fun withRepo(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("moru-git-diff").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun git(dir: File, vararg args: String): String {
        val process = ProcessBuilder(listOf("git", "-C", dir.absolutePath) + args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) { "git ${args.toList()} failed: $output" }
        return output.trim()
    }

    private fun initRepo(dir: File) {
        git(dir, "init", "-q", "-b", "main")
        git(dir, "config", "user.name", "Test Author")
        git(dir, "config", "user.email", "test@example.com")
        git(dir, "config", "commit.gpgsign", "false")
    }

    private fun commitAll(dir: File, message: String): String {
        git(dir, "add", "-A")
        git(dir, "commit", "-q", "-m", message)
        return git(dir, "rev-parse", "HEAD")
    }

    private fun files(dir: File, hash: String): List<DiffFile> =
        assertIs<CommitDiffResult.Loaded>(WorkbenchGitDiffScanner.commit(dir, hash)).files

    @Test
    fun theRootCommitShowsEveryFileAsAdded() = withRepo { dir ->
        initRepo(dir)
        File(dir, "a.txt").writeText("one\ntwo\n")
        val root = commitAll(dir, "root")

        val file = files(dir, root).single()

        assertEquals(DiffFileStatus.Added, file.status)
        assertEquals("a.txt", file.displayPath)
        assertEquals(2, file.additions)
    }

    @Test
    fun readsModifyAddDeleteAndRenameFromOneCommit() = withRepo { dir ->
        initRepo(dir)
        File(dir, "mod.txt").writeText("a\nb\nc\n")
        File(dir, "gone.txt").writeText("bye\n")
        File(dir, "old name.txt").writeText("same content\nfor rename detection\nplease\n")
        commitAll(dir, "base")

        File(dir, "mod.txt").writeText("a\nB\nc\nd\n")
        File(dir, "gone.txt").delete()
        File(dir, "new.txt").writeText("fresh\n")
        File(dir, "old name.txt").renameTo(File(dir, "renamed name.txt"))
        val change = commitAll(dir, "change")

        val byPath = files(dir, change).associateBy { file -> file.displayPath }

        assertEquals(DiffFileStatus.Modified, byPath.getValue("mod.txt").status)
        assertEquals(2, byPath.getValue("mod.txt").additions)
        assertEquals(1, byPath.getValue("mod.txt").deletions)
        assertEquals(DiffFileStatus.Deleted, byPath.getValue("gone.txt").status)
        assertEquals(DiffFileStatus.Added, byPath.getValue("new.txt").status)
        val renamed = byPath.getValue("renamed name.txt")
        assertEquals(DiffFileStatus.Renamed, renamed.status)
        assertEquals("old name.txt", renamed.oldPath)
    }

    @Test
    fun aNonAsciiFileNameIsNotQuoted() = withRepo { dir ->
        initRepo(dir)
        File(dir, "한글 파일.txt").writeText("내용\n")
        val hash = commitAll(dir, "korean")

        assertEquals("한글 파일.txt", files(dir, hash).single().displayPath)
    }

    @Test
    fun aMergeIsShownAgainstItsFirstParent() = withRepo { dir ->
        initRepo(dir)
        File(dir, "a.txt").writeText("a\n")
        commitAll(dir, "first")
        git(dir, "checkout", "-q", "-b", "feat")
        File(dir, "b.txt").writeText("b\n")
        commitAll(dir, "feature")
        git(dir, "checkout", "-q", "main")
        File(dir, "c.txt").writeText("c\n")
        commitAll(dir, "main work")
        git(dir, "merge", "-q", "--no-ff", "feat", "-m", "Merge feat")
        val merge = git(dir, "rev-parse", "HEAD")

        assertEquals(listOf("b.txt"), files(dir, merge).map { file -> file.displayPath })
    }

    @Test
    fun anOptionLikeHashIsRefusedBeforeGitSeesIt() = withRepo { dir ->
        initRepo(dir)

        assertIs<CommitDiffResult.Unavailable>(WorkbenchGitDiffScanner.commit(dir, "--output=/tmp/x"))
    }

    @Test
    fun anUnknownCommitIsUnavailableNotAnEmptyDiff() = withRepo { dir ->
        initRepo(dir)
        File(dir, "a.txt").writeText("a\n")
        commitAll(dir, "first")

        assertIs<CommitDiffResult.Unavailable>(WorkbenchGitDiffScanner.commit(dir, "deadbeefdeadbeef"))
    }

    @Test
    fun aMissingFolderIsUnavailable() {
        assertTrue(WorkbenchGitDiffScanner.commit(File("/no/such/folder"), "abcdef1") is CommitDiffResult.Unavailable)
    }

    private fun workingTree(dir: File): List<DiffFile> =
        assertIs<CommitDiffResult.Loaded>(WorkbenchGitDiffScanner.workingTree(dir)).files

    @Test
    fun aCleanWorkingTreeHasNoChanges() = withRepo { dir ->
        initRepo(dir)
        File(dir, "a.txt").writeText("a\n")
        commitAll(dir, "first")

        assertTrue(workingTree(dir).isEmpty())
    }

    @Test
    fun showsEditsStagedEditsDeletesAndNewFilesTogether() = withRepo { dir ->
        initRepo(dir)
        File(dir, "edited.txt").writeText("a\nb\n")
        File(dir, "staged.txt").writeText("x\n")
        File(dir, "gone.txt").writeText("bye\n")
        commitAll(dir, "base")

        File(dir, "edited.txt").writeText("a\nB\n")
        File(dir, "staged.txt").writeText("x\ny\n")
        git(dir, "add", "staged.txt")
        File(dir, "gone.txt").delete()
        File(dir, "brand new.txt").writeText("one\ntwo\n")

        val byPath = workingTree(dir).associateBy { file -> file.displayPath }

        assertEquals(DiffFileStatus.Modified, byPath.getValue("edited.txt").status)
        assertEquals(1, byPath.getValue("edited.txt").deletions)
        assertEquals(1, byPath.getValue("staged.txt").additions)
        assertEquals(DiffFileStatus.Deleted, byPath.getValue("gone.txt").status)
        val added = byPath.getValue("brand new.txt")
        assertEquals(DiffFileStatus.Added, added.status)
        assertEquals(2, added.additions)
    }

    @Test
    fun anIgnoredFileIsNotListedAsNew() = withRepo { dir ->
        initRepo(dir)
        File(dir, ".gitignore").writeText("secret.log\n")
        commitAll(dir, "base")
        File(dir, "secret.log").writeText("do not show\n")

        assertTrue(workingTree(dir).isEmpty())
    }

    @Test
    fun aRepositoryWithNoCommitsShowsItsFilesAsNew() = withRepo { dir ->
        initRepo(dir)
        File(dir, "first.txt").writeText("hello\n")

        val file = workingTree(dir).single()

        assertEquals(DiffFileStatus.Added, file.status)
        assertEquals("first.txt", file.displayPath)
    }

    @Test
    fun aFolderThatIsNotARepositoryIsUnavailable() {
        val dir = Files.createTempDirectory("moru-not-git").toFile()
        try {
            assertIs<CommitDiffResult.Unavailable>(WorkbenchGitDiffScanner.workingTree(dir))
        } finally {
            dir.deleteRecursively()
        }
    }
}
