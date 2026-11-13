package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.graphics.toComposeImageBitmap
import app.thdev.moru.core.domain.markdown.MarkdownImagePolicy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Byte and dimension limits are checked before allocating a decoded image, away from the UI thread. */
internal object DesktopMarkdownImageLoader : MarkdownImageLoader {
    internal const val MaxEncodedBytes = MarkdownImagePolicy.maximumEncodedBytes
    private const val MaxSourceDimension = MarkdownImagePolicy.maximumSourceDimension
    private const val MaxSourcePixels = MarkdownImagePolicy.maximumSourcePixels
    private const val MaxDecodedDimension = MarkdownImagePolicy.maximumDecodedDimension

    override suspend fun load(source: String, imageBasePath: String?): MarkdownImageLoadResult = withContext(Dispatchers.IO) {
        try {
            val location = resolveLocation(source, imageBasePath) ?: return@withContext MarkdownImageLoadResult.Unavailable
            val bytes = when (location) {
                is Location.Local -> {
                    if (!Files.isRegularFile(location.path) || Files.size(location.path) > MaxEncodedBytes) return@withContext MarkdownImageLoadResult.Unavailable
                    Files.newInputStream(location.path).use(::readBounded)
                }
                is Location.Web -> readWeb(location.uri)
            } ?: return@withContext MarkdownImageLoadResult.Unavailable
            decode(bytes, location.openTarget)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            MarkdownImageLoadResult.Unavailable
        }
    }

    internal sealed interface Location {
        val openTarget: String
        data class Local(val path: Path) : Location { override val openTarget: String get() = path.toString() }
        data class Web(val uri: URI) : Location { override val openTarget: String get() = uri.toASCIIString() }
    }

    internal fun resolveLocation(source: String, imageBasePath: String?): Location? {
        return when (MarkdownImagePolicy.sourceKind(source)) {
            "Web" -> URI(source).takeIf { !it.host.isNullOrBlank() && it.rawUserInfo == null }?.let { Location.Web(it) }
            "File" -> {
                val uri = URI(source)
                if (!uri.host.isNullOrBlank() && !uri.host.equals("localhost", ignoreCase = true)) null
                else Location.Local(Path.of(uri.path).toAbsolutePath().normalize())
            }
            "Path" -> {
                val decoded = runCatching { URI(source).path }.getOrNull() ?: source
                val path = Path.of(decoded)
                if (!path.isAbsolute && imageBasePath == null) null
                else Location.Local((if (path.isAbsolute) path else Path.of(imageBasePath!!).resolve(path)).toAbsolutePath().normalize())
            }
            else -> null
        }
    }

    internal fun readBounded(stream: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (output.size().toLong() + count > MaxEncodedBytes) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray().takeIf { it.isNotEmpty() }
    }

    private fun readWeb(initialUri: URI): ByteArray? {
        var uri = initialUri
        repeat(MarkdownImagePolicy.maximumRequests) {
            val connection = uri.toURL().openConnection() as? HttpURLConnection ?: return null
            try {
                connection.connectTimeout = MarkdownImagePolicy.connectTimeoutMillis
                connection.readTimeout = MarkdownImagePolicy.readTimeoutMillis
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "image/*")
                when (connection.responseCode) {
                    in 200..299 -> {
                        if (connection.contentLengthLong > MaxEncodedBytes) return null
                        return connection.inputStream.use(::readBounded)
                    }
                    301, 302, 303, 307, 308 -> {
                        val redirect = connection.getHeaderField("Location") ?: return null
                        uri = (resolveLocation(uri.resolve(redirect).toASCIIString(), null) as? Location.Web)?.uri ?: return null
                    }
                    else -> return null
                }
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    internal fun decode(bytes: ByteArray, openTarget: String): MarkdownImageLoadResult {
        if (bytes.isEmpty() || bytes.size > MaxEncodedBytes) return MarkdownImageLoadResult.Unavailable
        MemoryCacheImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return MarkdownImageLoadResult.Unavailable
            try {
                reader.input = stream
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width !in 1..MaxSourceDimension || height !in 1..MaxSourceDimension || width.toLong() * height > MaxSourcePixels) {
                    return MarkdownImageLoadResult.Unavailable
                }
                val sample = (maxOf(width, height) + MaxDecodedDimension - 1) / MaxDecodedDimension
                val parameters = reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) }
                val image = reader.read(0, parameters) ?: return MarkdownImageLoadResult.Unavailable
                return try {
                    MarkdownImageLoadResult.Available(image.toComposeImageBitmap(), openTarget)
                } finally {
                    image.flush()
                }
            } finally {
                reader.dispose()
            }
        }
    }
}
