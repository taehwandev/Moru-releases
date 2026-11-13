package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.SessionActivity
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/** Shared across windows, scoped to live session identities, and cleared when their sessions close. */
object SessionResponseAttention {
    private val registry = SessionResponseAttentionRegistry()
    private val awaitingOutput = ConcurrentHashMap.newKeySet<String>()
    val revision: StateFlow<Long> get() = registry.revision

    fun observe(sessionId: String, activity: SessionActivity): SessionActivity = registry.observe(sessionId, activity)

    /** Output timing is only a guess; it cannot start another turn after a finished response. */
    fun project(sessionId: String, activity: SessionActivity): SessionActivity =
        registry.observe(sessionId, activity, rearm = false)

    /** Called before submitted input reaches the PTY, so even its first output belongs to the new turn. */
    fun inputSubmitted(sessionId: String) {
        awaitingOutput.add(sessionId)
    }

    /** The first output after a submission is work, even inside the redraw reaction window. */
    internal fun outputReceived(sessionId: String): Boolean {
        if (!awaitingOutput.remove(sessionId)) return false
        registry.observe(sessionId, SessionActivity.Working)
        return true
    }

    fun acknowledge(sessionId: String): Boolean {
        val reported = SessionReportedActivities.activityFor(sessionId)
        if (reported != null && reported != SessionActivity.Quiet) return false
        if (SessionBusyScreens.isBusy(sessionId) || SessionQuestions.questionFor(sessionId) != null ||
            sessionId in SessionPermissions.requests.value) return false
        return registry.acknowledge(sessionId)
    }

    fun isAcknowledged(sessionId: String): Boolean = registry.isAcknowledged(sessionId)

    /** Seen responses, saved so a restart does not present them as new. */
    fun acknowledgedSessions(): Set<String> = registry.acknowledged()

    /** Seen responses saved before the restart; call before the sessions are first read. */
    fun restoreAcknowledged(sessionIds: Set<String>) = registry.restore(sessionIds)

    fun forget(sessionId: String) {
        awaitingOutput.remove(sessionId)
        registry.forget(sessionId)
    }
}
