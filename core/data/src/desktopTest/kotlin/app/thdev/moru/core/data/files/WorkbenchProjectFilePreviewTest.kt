package app.thdev.moru.core.data.files

import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkbenchProjectFilePreviewTest {
    // Every folder a test makes is removed after it, so runs do not pile up in the temp folder.
    private val created = mutableListOf<java.io.File>()
    private fun tempDir(prefix: String) = Files.createTempDirectory(prefix).toFile().also(created::add)

    @kotlin.test.AfterTest
    fun deleteTempDirs() = created.forEach { it.deleteRecursively() }

    @Test
    fun previewsUtf8TextInsideProjectRoot() {
        val root = tempDir("moru-preview-text")
        val file = root.resolve("docs/readme.md")
        file.parentFile.mkdirs()
        file.writeText("hello\nworld\n")

        val preview = root.openWorkbenchProjectFilePreview("docs/readme.md")

        assertEquals(WorkbenchFilePreviewStatus.TEXT, preview.status)
        assertEquals("readme.md", preview.name)
        assertEquals("docs/readme.md", preview.path)
        assertEquals("hello\nworld\n", preview.content)
        assertEquals(false, preview.truncated)
        assertTrue(preview.summaryText.contains("bytes"))
    }

    @Test
    fun rejectsPathOutsideProjectRoot() {
        val parent = tempDir("moru-preview-parent")
        val root = parent.resolve("project")
        root.mkdirs()
        parent.resolve("outside.txt").writeText("secret")

        val preview = root.openWorkbenchProjectFilePreview("../outside.txt")

        assertEquals(WorkbenchFilePreviewStatus.UNAVAILABLE, preview.status)
        assertNull(preview.content)
        assertTrue(preview.summaryText.contains("outside the active project"))
    }

    @Test
    fun hidesBinaryFiles() {
        val root = tempDir("moru-preview-binary")
        root.resolve("image.bin").writeBytes(byteArrayOf(0, 1, 2, 3, 4))

        val preview = root.openWorkbenchProjectFilePreview("image.bin")

        assertEquals(WorkbenchFilePreviewStatus.BINARY, preview.status)
        assertNull(preview.content)
        assertEquals("Binary file preview is disabled.", preview.summaryText)
    }

    @Test
    fun hidesOversizedFiles() {
        val root = tempDir("moru-preview-large")
        root.resolve("large.log").writeBytes(ByteArray(300 * 1024) { 'a'.code.toByte() })

        val preview = root.openWorkbenchProjectFilePreview("large.log")

        assertEquals(WorkbenchFilePreviewStatus.OVERSIZED, preview.status)
        assertNull(preview.content)
        assertEquals(true, preview.truncated)
        assertNotNull(preview.truncationText)
    }
}
