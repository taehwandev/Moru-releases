package app.thdev.moru.core.terminal.drop

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * An image, image link or file link dropped on a terminal is pasted as a path, so Claude Code and Codex attach it
 * the way they do when a file is dropped on Terminal.app.
 */
class TerminalDropReaderTest {
    private val folder = Files.createTempDirectory("moru-drop-test").toFile()

    @Test
    fun aDroppedBrowserImageIsSavedAndItsPathPasted() {
        val pasted = mutableListOf<String>()
        val reader = TerminalDropReader(pasted::add, dropFolder = { folder })

        assertTrue(reader.read(ImageDropped(BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB))))

        val path = File(pasted.single().trimEnd())
        assertEquals(folder, path.parentFile)
        assertEquals(3, ImageIO.read(path).width)
    }

    @Test
    fun aDroppedWebImageIsDownloadedAndOtherLinksStayLinks() {
        val pasted = mutableListOf<String>()
        val image = File(folder, "cat.png")
        val reader = TerminalDropReader(
            paste = pasted::add,
            dropFolder = { folder },
            download = { link, _ -> image.takeIf { link.path.endsWith(".png") } },
            inBackground = { it() },
        )

        assertTrue(reader.read(StringSelection("https://example.com/cat.png\r\nhttps://example.com/page\r\n")))
        SwingUtilities.invokeAndWait { }

        assertEquals("${image.absolutePath} https://example.com/page ", pasted.single())
    }

    @Test
    fun fileLinksBecomePathsAndPlainTextIsPastedAsIs() {
        val pasted = mutableListOf<String>()
        val reader = TerminalDropReader(pasted::add, dropFolder = { folder })

        reader.read(StringSelection("file:///tmp/a%20b.png"))
        reader.read(StringSelection("look at this"))

        assertEquals(listOf("/tmp/a\\ b.png ", "look at this"), pasted)
    }

    @Test
    fun onlyTextMadeOfLinksIsTreatedAsLinks() {
        assertEquals(listOf(URI("https://a.b/c.png")), droppedUris("# comment\nhttps://a.b/c.png\n"))
        assertNull(droppedUris("see https://a.b/c.png"))
        assertNull(droppedUris("ftp://a.b/c.png"))
        assertNotNull(droppedUris("file:///Users/me/x.png"))
    }

    @Test
    fun shellPathEscapesOnlyWhatAShellWouldSplitOrExpand() {
        assertEquals("/tmp/한글/a\\ \\'b\\'\\&\\$.png", shellPath(File("/tmp/한글/a 'b'&$.png")))
    }

    private class ImageDropped(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
        override fun getTransferData(flavor: DataFlavor): Any = image
    }
}
