package app.thdev.moru.core.terminal.drop

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import javax.imageio.ImageIO

internal fun defaultDropFolder(): File = File(System.getProperty("java.io.tmpdir"), "moru-drops").apply { mkdirs() }

internal fun saveImage(image: Image, folder: File): File {
    val buffered = image as? BufferedImage ?: BufferedImage(
        image.getWidth(null).coerceAtLeast(1),
        image.getHeight(null).coerceAtLeast(1),
        BufferedImage.TYPE_INT_ARGB,
    ).also { copy ->
        val graphics = copy.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
    }
    return File.createTempFile("image-", ".png", folder).also { ImageIO.write(buffered, "png", it) }
}

private const val maxDownloadBytes = 20L * 1024 * 1024

private val imageExtensions = mapOf(
    "image/png" to "png",
    "image/jpeg" to "jpg",
    "image/gif" to "gif",
    "image/webp" to "webp",
    "image/svg+xml" to "svg",
    "image/bmp" to "bmp",
    "image/heic" to "heic",
)

/** The web image at [link] saved in [folder], or null when it is not an image or cannot be fetched. */
internal fun downloadImage(link: URI, folder: File): File? = runCatching {
    val connection = link.toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 15_000
    connection.instanceFollowRedirects = true
    try {
        if (connection.responseCode !in 200..299) return null
        val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase() ?: return null
        val extension = imageExtensions[type] ?: return null
        if (connection.contentLengthLong > maxDownloadBytes) return null
        val file = File.createTempFile("image-", ".$extension", folder)
        val copied = connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
        if (copied > maxDownloadBytes) {
            file.delete()
            null
        } else {
            file
        }
    } finally {
        connection.disconnect()
    }
}.getOrNull()
