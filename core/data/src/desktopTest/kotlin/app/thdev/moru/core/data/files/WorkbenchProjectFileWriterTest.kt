package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorkbenchProjectFileWriterTest {
    // Every folder a test makes is removed after it, so runs do not pile up in the temp folder.
    private val created = mutableListOf<java.io.File>()
    private fun tempDir(prefix: String) = Files.createTempDirectory(prefix).toFile().also(created::add)

    @kotlin.test.AfterTest
    fun deleteTempDirs() = created.forEach { it.deleteRecursively() }

    @Test
    fun savesEditedTextToExistingFileInsideRoot() {
        val root = tempDir("moru-save-text")
        val file = root.resolve("docs/readme.md")
        file.parentFile.mkdirs()
        file.writeText("hello\n")

        val outcome = root.saveWorkbenchProjectFile("docs/readme.md", "hello world\n")

        val saved = assertIs<WorkbenchFileSaveOutcome.Saved>(outcome)
        assertEquals("docs/readme.md", saved.path)
        assertEquals("hello world\n", file.readText())
        assertEquals(file.length(), saved.bytesWritten)
    }

    @Test
    fun rejectsPathOutsideProjectRoot() {
        val parent = tempDir("moru-save-parent")
        val root = parent.resolve("project").apply { mkdirs() }
        val outside = parent.resolve("outside.txt")
        outside.writeText("secret")

        val outcome = root.saveWorkbenchProjectFile("../outside.txt", "tampered")

        val failed = assertIs<WorkbenchFileSaveOutcome.Failed>(outcome)
        assertTrue(failed.message.contains("outside the active project"))
        assertEquals("secret", outside.readText())
    }

    @Test
    fun refusesToCreateNewFile() {
        val root = tempDir("moru-save-missing")

        val outcome = root.saveWorkbenchProjectFile("new.txt", "content")

        val failed = assertIs<WorkbenchFileSaveOutcome.Failed>(outcome)
        assertTrue(failed.message.contains("does not exist"))
        assertTrue(!root.resolve("new.txt").exists())
    }

    @Test
    fun refusesToOverwriteDirectory() {
        val root = tempDir("moru-save-dir")
        root.resolve("nested").mkdirs()

        val outcome = root.saveWorkbenchProjectFile("nested", "content")

        assertIs<WorkbenchFileSaveOutcome.Failed>(outcome)
    }
}
