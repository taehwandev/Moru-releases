package app.thdev.moru.core.terminal

import java.net.URI

/**
 * Only a web page served from this machine may be opened as a preview. The address came from
 * terminal output, so it is checked again here before anything is launched.
 */
fun isLocalPreviewUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val host = uri.host?.removePrefix("[")?.removeSuffix("]") ?: return false
    return uri.scheme in setOf("http", "https") && host in setOf("localhost", "127.0.0.1", "::1")
}
