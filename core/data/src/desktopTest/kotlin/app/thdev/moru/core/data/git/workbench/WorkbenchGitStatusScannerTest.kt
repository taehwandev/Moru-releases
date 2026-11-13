package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.data.git.GitTestRepository
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Runs the scanner against a real git repository. */
class WorkbenchGitStatusScannerTest {
    @Test
    fun aStatusLongerThanThePipeHoldsIsReadWhole() {
        val dir = Files.createTempDirectory("moru-git-status").toFile()
        try {
            val init = ProcessBuilder("git", "-C", dir.absolutePath, "init", "-q").redirectErrorStream(true).start()
            check(init.waitFor(30, TimeUnit.SECONDS) && init.exitValue() == 0)
            // About 3000 lines of `?? untracked-file-with-a-long-name-NNNN.txt`, well over a 64KB pipe.
            repeat(3000) { n -> dir.resolve("untracked-file-with-a-long-name-${n.toString().padStart(4, '0')}.txt").writeText("") }

            val status = WorkbenchGitStatusScanner.scan(dir)

            assertTrue(status.repositoryDetected, status.message)
            assertEquals(3000, status.changes.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun drainsStatusLargerThanTheProcessPipeWhileGitRuns() = GitTestRepository().use { repo ->
        repo.commit()
        val names = addUntrackedFiles(repo, 3_000)

        val status = WorkbenchGitStatusScanner.scan(repo.root.toFile())

        assertTrue(status.repositoryDetected, status.message)
        assertEquals("main", status.branchName)
        assertEquals(names.toSet(), status.changes.map { it.path }.toSet())
        assertEquals(names.size, status.untrackedCount)
    }

    @Test
    fun refusesStatusBeyondTheOutputBoundInsteadOfParsingPartialChanges() = GitTestRepository().use { repo ->
        repo.commit()
        addUntrackedFiles(repo, 22_000)

        val status = WorkbenchGitStatusScanner.scan(repo.root.toFile())

        assertFalse(status.repositoryDetected)
        assertTrue(status.changes.isEmpty())
        val message = assertNotNull(status.message)
        assertTrue(message.contains("safe bounds"), message)
    }

    @Test
    fun aCleanRepositoryStillReportsItsBranch() = GitTestRepository().use { repo ->
        repo.commit()

        val status = WorkbenchGitStatusScanner.scan(repo.root.toFile())

        assertTrue(status.repositoryDetected, status.message)
        assertEquals("main", status.branchName)
        assertTrue(status.changes.isEmpty())
        assertEquals("Working tree clean.", status.message)
    }

    @Test
    fun aFolderOutsideTheRepositoryRemainsUnavailable() = GitTestRepository().use { repo ->
        val status = WorkbenchGitStatusScanner.scan(repo.base.toFile())

        assertFalse(status.repositoryDetected)
        assertTrue(status.changes.isEmpty())
        assertTrue(status.message.orEmpty().contains("not a git repository"))
    }

    @Test
    fun aMissingFolderRemainsUnavailable() = GitTestRepository().use { repo ->
        assertEquals(WorkbenchGitStatus.NotRepository, WorkbenchGitStatusScanner.scan(repo.base.resolve("missing").toFile()))
    }

    private fun addUntrackedFiles(repo: GitTestRepository, count: Int): List<String> =
        List(count) { index -> "change-${index.toString().padStart(5, '0')}-${"x".repeat(220)}.txt" }
            .onEach { name -> repo.write(name, "") }
}
