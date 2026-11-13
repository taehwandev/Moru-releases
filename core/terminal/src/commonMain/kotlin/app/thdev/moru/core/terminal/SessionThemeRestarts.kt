package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** A session owns an explicit restart, so navigating away cannot cancel it after stopping the agent. */
object SessionThemeRestarts {
    enum class State { Running, Failed, Applied }
    private class Request(@Volatile var state: State = State.Running)
    private val requests = ConcurrentHashMap<String, Request>()

    fun state(sessionId: String): State? = requests[sessionId]?.state

    @Synchronized
    fun request(
        sessionId: String,
        executable: String,
        command: String,
        canRestart: () -> Boolean,
        submitLine: (String, String) -> Boolean,
    ): Boolean {
        if (state(sessionId) == State.Running) return false
        val request = Request()
        requests[sessionId] = request
        thread(name = "moru-theme-restart", isDaemon = true) {
            val applied = runCatching {
                if (!restartSessionAgent(sessionId, executable, command, canRestart, submitLine)) return@runCatching false
                val deadline = System.nanoTime() + 10_000_000_000L
                while (SessionThemeColors.hasStaleBackground(sessionId) && System.nanoTime() < deadline) Thread.sleep(50)
                !SessionThemeColors.hasStaleBackground(sessionId)
            }.getOrDefault(false)
            request.state = if (applied) State.Applied else State.Failed
        }
        return true
    }

    fun forget(sessionId: String) {
        requests.remove(sessionId)
    }
}
