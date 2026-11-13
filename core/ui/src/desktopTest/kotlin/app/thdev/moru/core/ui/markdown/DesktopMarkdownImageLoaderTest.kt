package app.thdev.moru.core.ui.markdown

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMarkdownImageLoaderTest {
    @Test
    fun localRelativeAndFileUriImagesResolveToTheSameOpenTarget() = runBlocking {
        val folder = Files.createTempDirectory("markdown-images")
        val file = folder.resolve("mock up.png")
        try {
            Files.write(file, png(8, 4))
            listOf(file.toString(), file.toUri().toString(), "mock%20up.png").forEach { source ->
                val image = assertIs<MarkdownImageLoadResult.Available>(DesktopMarkdownImageLoader.load(source, folder.toString()))
                assertEquals(file.toString(), image.openTarget)
                assertEquals(8, image.bitmap.width)
                assertEquals(4, image.bitmap.height)
            }
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(folder)
        }
    }

    @Test
    fun unknownSchemesRemoteFileAuthoritiesAndUnbasedRelativePathsAreUnavailable() = runBlocking {
        val authenticatedUri = java.net.URI("https", "fixture:fixture", "example.org", -1, "/image.png", null, null).toASCIIString()
        listOf("javascript:alert(1)", "data:image/png;base64,123", "file://remote-host/image.png", "relative.png", authenticatedUri).forEach {
            assertEquals(MarkdownImageLoadResult.Unavailable, DesktopMarkdownImageLoader.load(it, null))
        }
        assertIs<DesktopMarkdownImageLoader.Location.Web>(DesktopMarkdownImageLoader.resolveLocation("https://example.org/image.png", null))
        Unit
    }

    @Test
    fun missingAndCorruptFilesAreUnavailable() = runBlocking {
        val file = Files.createTempFile("markdown-corrupt", ".png")
        try {
            Files.writeString(file, "This is not an image.")
            assertEquals(MarkdownImageLoadResult.Unavailable, DesktopMarkdownImageLoader.load(file.toString(), null))
            assertEquals(MarkdownImageLoadResult.Unavailable, DesktopMarkdownImageLoader.load(file.resolveSibling("missing.png").toString(), null))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun unknownLengthStreamsStopAtTheEncodedByteLimit() {
        var bytesRead = 0
        val stream = object : InputStream() {
            override fun read(): Int { bytesRead++; return 0 }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                bytesRead += length
                buffer.fill(0, offset, offset + length)
                return length
            }
        }
        assertNull(DesktopMarkdownImageLoader.readBounded(stream))
        assertTrue(bytesRead <= DesktopMarkdownImageLoader.MaxEncodedBytes + 8192)
        assertEquals(listOf<Byte>(1, 2, 3), DesktopMarkdownImageLoader.readBounded(ByteArrayInputStream(byteArrayOf(1, 2, 3)))?.toList())
    }

    @Test
    fun decodingSamplesLargeImagesAndRejectsExcessiveSourceDimensions() {
        val sampled = assertIs<MarkdownImageLoadResult.Available>(DesktopMarkdownImageLoader.decode(png(2000, 100), "preview.png"))
        assertEquals(1000, sampled.bitmap.width)
        assertEquals(50, sampled.bitmap.height)
        assertEquals(MarkdownImageLoadResult.Unavailable, DesktopMarkdownImageLoader.decode(png(8193, 1), "wide.png"))
    }

    private fun png(width: Int, height: Int): ByteArray = ByteArrayOutputStream().use { output ->
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        try { ImageIO.write(image, "png", output) } finally { image.flush() }
        output.toByteArray()
    }
}
