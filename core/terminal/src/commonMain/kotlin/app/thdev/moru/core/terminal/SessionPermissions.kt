package app.thdev.moru.core.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/** One live source for all permission controls. Dispatch never runs while the registry lock is held. */
object SessionPermissions {
    private val lock = Any()
    private val sequence = AtomicLong()
    private val entries = mutableMapOf<String, PermissionObservation>()
    private val state = MutableStateFlow<Map<String, SessionPermissionRequest>>(emptyMap())
    val requests: StateFlow<Map<String, SessionPermissionRequest>> = state.asStateFlow()

    fun observe(sessionId: String, prompt: SessionPermissionPrompt?, respond: (String) -> Boolean = { false }) {
        synchronized(lock) {
            if (prompt == null) entries.remove(sessionId)
            else {
                val previous = entries[sessionId]
                if (previous?.prompt == prompt) return
                val request = SessionPermissionRequest(sequence.incrementAndGet().toString(), prompt.question, prompt.details, prompt.choices)
                entries[sessionId] = PermissionObservation(prompt, request, respond)
            }
            publish()
        }
    }

    /** Only the exact shown request and choice can be consumed, including concurrent clicks on other surfaces. */
    fun respond(sessionId: String, requestId: String, choiceId: String): Boolean {
        val entry = synchronized(lock) {
            val current = entries[sessionId] ?: return false
            if (current.consumed || current.request.id != requestId || current.request.choices.none { it.id == choiceId }) return false
            current.consumed = true
            publish()
            current
        }
        val sent = runCatching { entry.respond(choiceId) }.getOrDefault(false)
        synchronized(lock) {
            if (!sent && entries[sessionId] === entry) {
                entry.consumed = false
                publish()
            }
        }
        return sent
    }

    fun forget(sessionId: String) = observe(sessionId, null)

    private fun publish() {
        state.value = entries.filterValues { !it.consumed }.mapValues { it.value.request }
    }
}

private class PermissionObservation(
    val prompt: SessionPermissionPrompt,
    val request: SessionPermissionRequest,
    val respond: (String) -> Boolean,
    var consumed: Boolean = false,
)
