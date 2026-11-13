package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory acknowledgment of an observed agent turn, never Task completion or terminal output mutation. */
class SessionResponseAttentionRegistry {
    private data class Entry(val activity: SessionActivity, val finished: Boolean = false, val acknowledged: Boolean = false)

    private val lock = Any()
    private val entries = mutableMapOf<String, Entry>()
    private val changes = MutableStateFlow(0L)
    val revision: StateFlow<Long> = changes.asStateFlow()

    /** Repeated samples of the same completed turn preserve acknowledgment; actual work rearms the next turn. */
    fun observe(sessionId: String, activity: SessionActivity, rearm: Boolean = true): SessionActivity = synchronized(lock) {
        val previous = entries[sessionId]
        val observed = if (!rearm && previous?.finished == true &&
            (activity == SessionActivity.Working || activity == SessionActivity.Starting)) SessionActivity.Quiet else activity
        val next = when (observed) {
            SessionActivity.Working, SessionActivity.Starting ->
                if (rearm) Entry(observed) else previous?.copy(activity = observed) ?: Entry(observed)
            SessionActivity.Quiet -> previous?.copy(activity = observed, finished = true) ?: Entry(observed, finished = true)
            else -> previous?.copy(activity = observed) ?: Entry(observed)
        }
        entries[sessionId] = next
        // A new turn ends what the user had seen; saved acknowledgments must learn that too.
        if (previous?.acknowledged == true && !next.acknowledged) changes.value += 1
        if (observed == SessionActivity.Quiet && next.acknowledged) SessionActivity.Waiting else observed
    }

    /** The sessions whose finished response the user has seen, to keep across a restart. */
    fun acknowledged(): Set<String> = synchronized(lock) {
        entries.filterValues { it.finished && it.acknowledged }.keys.toSet()
    }

    /**
     * Seen responses saved before a restart. The retained terminals come back quiet, and their first redraw must not
     * present an answer the user already read as new; only a session not observed at work yet is restored.
     */
    fun restore(sessionIds: Set<String>) = synchronized(lock) {
        sessionIds.forEach { sessionId ->
            val entry = entries[sessionId]
            if (entry == null || entry.activity !in setOf(SessionActivity.Working, SessionActivity.Starting)) {
                entries[sessionId] = (entry ?: Entry(SessionActivity.Quiet)).copy(finished = true, acknowledged = true)
            }
        }
        changes.value += 1
    }

    /** Only a finished response can be acknowledged. Busy, permission and failure states remain authoritative. */
    fun acknowledge(sessionId: String): Boolean = synchronized(lock) {
        val entry = entries[sessionId] ?: return false
        if (entry.activity != SessionActivity.Quiet || !entry.finished || entry.acknowledged) return false
        entries[sessionId] = entry.copy(acknowledged = true)
        changes.value += 1
        true
    }

    fun isAcknowledged(sessionId: String): Boolean = synchronized(lock) {
        entries[sessionId]?.let { it.finished && it.acknowledged } == true
    }

    fun forget(sessionId: String) = synchronized(lock) {
        if (entries.remove(sessionId) != null) changes.value += 1
    }
}
