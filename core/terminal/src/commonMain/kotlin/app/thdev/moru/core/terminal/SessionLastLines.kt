package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * The newest meaningful line each terminal session has printed. Output is read on the PTY thread and the
 * UI reads this on its own timer, so this is a small concurrent map and not part of the session snapshot,
 * which must not change on every output chunk.
 */
object SessionLastLines {
    private val lines = ConcurrentHashMap<String, String>()

    fun record(sessionId: String, line: String) {
        lines[sessionId] = line
    }

    fun lineFor(sessionId: String): String? = lines[sessionId]

    fun forget(sessionId: String) {
        lines.remove(sessionId)
    }
}
