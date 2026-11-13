package app.thdev.moru.app

import app.thdev.moru.core.terminal.SESSION_WORKING_WINDOW_MILLIS
import kotlin.time.Clock

/**
 * Announces each reason once per Task attention episode. Terminal redraws can briefly hide a question or make it
 * look quiet, so an episode ends only after attention stays absent for the session's working window. Silence after a
 * question is still the same wait. The Task visible in a focused window is marked announced without an OS alert.
 */
internal class AttentionPolicy(private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {
    private class Episode {
        val announced = mutableSetOf<AttentionKind>()
        var absentSince: Long? = null
        val permissionRequests = mutableSetOf<Pair<String?, String>>()
    }

    private val episodes = mutableMapOf<String, Episode>()

    /** The Tasks whose episode the last [update] ended: they no longer need the user. */
    var ended: Set<String> = emptySet()
        private set

    /** Takes the Tasks that need attention now and returns the ones to announce now. */
    fun update(candidates: List<AttentionCandidate>, viewedTaskId: String?, windowFocused: Boolean): List<AttentionCandidate> {
        val now = nowMillis()
        val currentTasks = candidates.map { it.taskId }.toSet()
        val endedNow = mutableSetOf<String>()
        episodes.entries.removeAll { (taskId, episode) ->
            if (taskId !in currentTasks && episode.absentSince == null) episode.absentSince = now
            val ended = episode.absentSince?.let { now - it >= SESSION_WORKING_WINDOW_MILLIS } ?: false
            if (taskId in currentTasks) episode.absentSince = null
            if (ended) endedNow += taskId
            ended
        }
        ended = endedNow
        val fresh = candidates.filter { candidate ->
            val episode = episodes.getOrPut(candidate.taskId) { Episode() }
            val first = episode.announced.add(candidate.kind)
            val newPermission = candidate.permissionRequestId?.let { id ->
                episode.permissionRequests.add(candidate.sessionId to id)
            } ?: false
            if (candidate.kind == AttentionKind.Asking) episode.announced += AttentionKind.Finished
            first || newPermission
        }
        return fresh.filterNot { candidate -> windowFocused && candidate.taskId == viewedTaskId }
    }
}
