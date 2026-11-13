package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkbenchProjectFileCreatorTest {
    // Every folder a test makes is removed after it, so runs do not pile up in the temp folder.
    private val created = mutableListOf<java.io.File>()
    private fun tempDir(prefix: String) = Files.createTempDirectory(prefix).toFile().also(created::add)

    @kotlin.test.AfterTest
    fun deleteTempDirs() = created.forEach { it.deleteRecursively() }

    @Test
    fun createsEmptyFileInsideRootAndParents() {
        val root = tempDir("moru-create")

        val outcome = root.createWorkbenchProjectFile("notes/Ideas.md")

        assertIs<WorkbenchFileCreateOutcome.Created>(outcome)
        val created = root.resolve("notes/Ideas.md")
        assertTrue(created.isFile)
        assertEquals("", created.readText())
    }

    @Test
    fun refusesToOverwriteExistingFile() {
        val root = tempDir("moru-create-existing")
        root.resolve("note.md").writeText("keep me")

        val outcome = root.createWorkbenchProjectFile("note.md")

        val failed = assertIs<WorkbenchFileCreateOutcome.Failed>(outcome)
        assertTrue(failed.message.contains("already exists"))
        assertEquals("keep me", root.resolve("note.md").readText())
    }

    @Test
    fun rejectsPathOutsideRoot() {
        val parent = tempDir("moru-create-parent")
        val root = parent.resolve("project").apply { mkdirs() }

        val outcome = root.createWorkbenchProjectFile("../escape.md")

        assertIs<WorkbenchFileCreateOutcome.Failed>(outcome)
        assertTrue(!parent.resolve("escape.md").exists())
    }
}
