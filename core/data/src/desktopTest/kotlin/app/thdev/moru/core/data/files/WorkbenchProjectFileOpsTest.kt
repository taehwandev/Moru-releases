package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkbenchProjectFileOpsTest {
    // Every folder a test makes is removed after it, so runs do not pile up in the temp folder.
    private val created = mutableListOf<java.io.File>()
    private fun tempDir(prefix: String) = Files.createTempDirectory(prefix).toFile().also(created::add)

    @kotlin.test.AfterTest
    fun deleteTempDirs() = created.forEach { it.deleteRecursively() }

    @Test
    fun renameMovesFileContentToNewPath() {
        val root = tempDir("moru-rename")
        root.resolve("Old.md").writeText("body")

        val outcome = root.renameWorkbenchProjectFile("Old.md", "notes/New.md")

        assertIs<WorkbenchFileRenameOutcome.Renamed>(outcome)
        assertTrue(!root.resolve("Old.md").exists())
        assertEquals("body", root.resolve("notes/New.md").readText())
    }

    @Test
    fun renameRefusesToOverwriteExistingTarget() {
        val root = tempDir("moru-rename-clobber")
        root.resolve("a.md").writeText("a")
        root.resolve("b.md").writeText("b")

        val outcome = root.renameWorkbenchProjectFile("a.md", "b.md")

        assertIs<WorkbenchFileRenameOutcome.Failed>(outcome)
        assertEquals("a", root.resolve("a.md").readText())
        assertEquals("b", root.resolve("b.md").readText())
    }

    @Test
    fun renameRejectsTargetOutsideRoot() {
        val parent = tempDir("moru-rename-escape")
        val root = parent.resolve("project").apply { mkdirs() }
        root.resolve("note.md").writeText("x")

        val outcome = root.renameWorkbenchProjectFile("note.md", "../escaped.md")

        assertIs<WorkbenchFileRenameOutcome.Failed>(outcome)
        assertTrue(!parent.resolve("escaped.md").exists())
    }

    @Test
    fun deleteRemovesExistingFile() {
        val root = tempDir("moru-delete")
        root.resolve("gone.md").writeText("x")

        val outcome = root.deleteWorkbenchProjectFile("gone.md")

        assertIs<WorkbenchFileDeleteOutcome.Deleted>(outcome)
        assertTrue(!root.resolve("gone.md").exists())
    }

    @Test
    fun deleteRefusesDirectories() {
        val root = tempDir("moru-delete-dir")
        root.resolve("folder").mkdirs()

        val outcome = root.deleteWorkbenchProjectFile("folder")

        assertIs<WorkbenchFileDeleteOutcome.Failed>(outcome)
        assertTrue(root.resolve("folder").isDirectory)
    }
}
