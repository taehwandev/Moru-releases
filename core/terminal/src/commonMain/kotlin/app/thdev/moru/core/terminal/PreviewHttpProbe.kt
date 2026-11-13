package app.thdev.moru.core.terminal

import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI

/**
 * Whether a local listener serves a page a Preview can show. Agents and their tools listen too: an MCP login callback
 * answers 400, Codex's own servers 401, Gradle or a debugger a private protocol; none of those is a page. A page is a
 * 2xx or 3xx answer to `GET /`, or an error answered with HTML (a dev server showing its error overlay). Dev servers that
 * listen on `localhost` may take only IPv6 (Vite on recent Node), so `::1` is asked when `127.0.0.1` does not answer.
 */
internal fun respondsToPreviewHttp(port: Int): Boolean {
    if (port !in 1..65535) return false
    return servesPage("127.0.0.1", port) ?: servesPage("[::1]", port) ?: false
}

/** True or false when [host] answered HTTP on [port]; null when nothing answered there. */
private fun servesPage(host: String, port: Int): Boolean? {
    val connection = URI("http://$host:$port/").toURL().openConnection(Proxy.NO_PROXY) as HttpURLConnection
    return try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 250
        connection.readTimeout = 250
        // A redirect must stay on this exact socket.
        connection.instanceFollowRedirects = false
        // useCaches stays at its default: turning it off adds `Cache-Control: no-cache`, which makes a dev server skip
        // its data cache and redo every read the page does. No ResponseCache is installed, so nothing is cached anyway.
        val status = connection.responseCode
        if (status !in 100..599) return null
        status in 200..399 || connection.contentType.orEmpty().startsWith("text/html", ignoreCase = true)
    } catch (_: java.io.IOException) {
        // Refused, an empty reply, another protocol, or a timeout: no web server here.
        null
    } finally {
        connection.disconnect()
    }
}
