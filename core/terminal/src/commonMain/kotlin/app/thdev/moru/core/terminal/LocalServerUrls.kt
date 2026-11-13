package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * The newest local server address each terminal session has printed. Terminal output is read on
 * the PTY thread while the UI reads this on its own timer, so this is a small concurrent map and
 * not part of the session snapshot, which must not change on every output chunk.
 */
object LocalServerUrls {
    private val urls = ConcurrentHashMap<String, String>()

    fun record(sessionId: String, url: String) {
        urls[sessionId] = url
    }

    fun urlFor(sessionId: String): String? = urls[sessionId]

    fun forget(sessionId: String) {
        urls.remove(sessionId)
    }
}
