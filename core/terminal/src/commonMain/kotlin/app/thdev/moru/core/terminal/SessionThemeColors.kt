package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/** Live color observations only. A theme change never stops or sends a command to a session. */
object SessionThemeColors {
    private data class Reading(val canvas: Int, val queriedCanvas: Int? = null)
    private val readings = ConcurrentHashMap<String, Reading>()

    /** The background the terminal currently answers when its program asks for OSC 11. */
    fun recordCanvas(sessionId: String, rgb: Int) {
        readings.compute(sessionId) { _, previous -> Reading(rgb, previous?.queriedCanvas) }
    }

    /** The exact background reply sent to the program, including after an explicit restart. */
    fun recordBackgroundColor(sessionId: String, rgb: Int) {
        readings.computeIfPresent(sessionId) { _, current -> current.copy(queriedCanvas = rgb) }
    }

    fun hasStaleBackground(sessionId: String): Boolean = readings[sessionId]?.let { reading ->
        reading.queriedCanvas != null && reading.queriedCanvas != reading.canvas
    } ?: false

    fun forget(sessionId: String) {
        readings.remove(sessionId)
        SessionThemeRestarts.forget(sessionId)
    }
}
