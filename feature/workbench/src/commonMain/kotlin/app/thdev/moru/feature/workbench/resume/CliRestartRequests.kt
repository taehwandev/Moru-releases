package app.thdev.moru.feature.workbench.resume

import androidx.compose.runtime.mutableStateMapOf
import app.thdev.moru.feature.workbench.launch.AgentRuntime

/**
 * Agent sessions whose CLI was updated but which Moru would not restart on its own, by session id, so their Task's
 * terminal can ask the user. App-wide because the restarts are decided for every Task at once.
 */
object CliRestartRequests {
    private val requests = mutableStateMapOf<String, AgentRuntime>()

    fun ask(sessionId: String, runtime: AgentRuntime) {
        requests[sessionId] = runtime
    }

    fun clear(sessionId: String) {
        requests.remove(sessionId)
    }

    /** The first request among [sessionIds], in their order, or null. */
    fun firstAmong(sessionIds: List<String>): Pair<String, AgentRuntime>? =
        sessionIds.firstNotNullOfOrNull { id -> requests[id]?.let { runtime -> id to runtime } }
}
