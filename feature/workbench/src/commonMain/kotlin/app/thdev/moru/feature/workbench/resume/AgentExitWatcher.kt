package app.thdev.moru.feature.workbench.resume

/**
 * Decides when an agent in a terminal that stays open has quit, from one reading per session at a time: whether the
 * terminal's shell is running a program. A session counts as exited only after a program was seen running and the
 * shell then stayed at its prompt for [confirmations] readings in a row, so the moment before a command starts, or a
 * single flicker between programs, never looks like an exit. A reading of null (unknown) changes nothing.
 */
class AgentExitWatcher(private val confirmations: Int = 2) {
    private val seenRunning = mutableSetOf<String>()
    private val idleReadings = mutableMapOf<String, Int>()
    private val exited = mutableSetOf<String>()

    /** Takes the newest reading for [sessionId]; returns whether it now counts as exited. */
    fun observe(sessionId: String, running: Boolean?): Boolean {
        when (running) {
            true -> {
                seenRunning += sessionId
                idleReadings.remove(sessionId)
                exited -= sessionId
            }
            false -> if (sessionId in seenRunning) {
                val idle = (idleReadings[sessionId] ?: 0) + 1
                idleReadings[sessionId] = idle
                if (idle >= confirmations) exited += sessionId
            }
            null -> Unit
        }
        return sessionId in exited
    }

    /** The user resumed or dismissed it: it must be seen running again before it can count as exited. */
    fun acknowledge(sessionId: String) {
        exited -= sessionId
        seenRunning -= sessionId
        idleReadings.remove(sessionId)
    }

    /** A session that no longer exists is forgotten. */
    fun retainOnly(sessionIds: Set<String>) {
        seenRunning.retainAll(sessionIds)
        idleReadings.keys.retainAll(sessionIds)
        exited.retainAll(sessionIds)
    }

    fun exitedSessions(): Set<String> = exited.toSet()
}
