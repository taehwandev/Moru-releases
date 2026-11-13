package app.thdev.moru.core.terminal

/**
 * A background process reading shared by every session view. Reading a row must never enumerate OS processes.
 * A missing reading means unknown; commands still use a fresh process query before acting.
 */
internal class SessionProgramObservations(
    private val shells: () -> Map<String, Long>,
    private val currentPid: (String) -> Long?,
    private val query: (String) -> Boolean?,
) {
    private data class Reading(val pid: Long, val running: Boolean?)

    @Volatile private var readings: Map<String, Reading> = emptyMap()

    fun readingOf(sessionId: String): Boolean? {
        val reading = readings[sessionId] ?: return null
        return reading.running.takeIf { currentPid(sessionId) == reading.pid }
    }

    /** Called only by the shared session clock's background collector, before publishing its next tick. */
    fun refresh() {
        readings = shells().mapValues { (id, pid) ->
            Reading(pid, runCatching { query(id) }.getOrNull())
        }
    }
}
