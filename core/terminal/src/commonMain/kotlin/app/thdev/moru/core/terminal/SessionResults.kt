package app.thdev.moru.core.terminal

import app.thdev.moru.core.model.session.ConversationTurn
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What each session's agent said when its turn ended, read for free from what is already on hand: the screen, a Lab
 * conversation, or the transcript the agent CLI keeps. A transcript holds the whole message, so it outranks the screen
 * until the screen shows a different message, which starts a new turn. Each session also leaves how a reply reaches it,
 * so the Results view can answer without opening the terminal.
 */
object SessionResults {
    private val screen = ConcurrentHashMap<String, String>()
    private val transcripts = ConcurrentHashMap<String, String>()
    private val senders = ConcurrentHashMap<String, (String) -> Boolean>()
    private val state = MutableStateFlow<Map<String, String>>(emptyMap())
    private val changeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val threadState = MutableStateFlow<Map<String, List<ConversationTurn>>>(emptyMap())

    /** The newest result by session id. */
    val results: StateFlow<Map<String, String>> = state.asStateFlow()

    /** The screen's reading of the last message; one the screen has not shown before drops the older transcript. */
    fun recordScreen(sessionId: String, text: String?) {
        if (text == null) return
        val previous = screen.put(sessionId, text)
        if (previous != text) transcripts.remove(sessionId)
        publish()
    }

    fun recordTranscript(sessionId: String, text: String?) {
        if (text.isNullOrBlank()) transcripts.remove(sessionId) else transcripts[sessionId] = text.trim()
        publish()
    }

    fun screenFor(sessionId: String): String? = screen[sessionId]

    fun resultFor(sessionId: String): String? = transcripts[sessionId] ?: screen[sessionId]

    /**
     * How many files stand uncommitted in the checkout the session works in, read when its result arrived: the
     * evidence of what changed. Shared by every session in that checkout; null when it could not be read.
     */
    val changes: StateFlow<Map<String, Int>> = changeCounts.asStateFlow()

    fun recordChanges(sessionId: String, count: Int?) {
        changeCounts.value = if (count == null) changeCounts.value - sessionId else changeCounts.value + (sessionId to count)
    }

    /**
     * The exchanges that led to the result, oldest first, read from the same transcript or Lab conversation; a session
     * whose record cannot be read has none, and the Results view shows its result alone.
     */
    val threads: StateFlow<Map<String, List<ConversationTurn>>> = threadState.asStateFlow()

    fun recordThread(sessionId: String, turns: List<ConversationTurn>) {
        threadState.value = if (turns.isEmpty()) threadState.value - sessionId else threadState.value + (sessionId to turns)
    }

    fun registerSender(sessionId: String, send: (String) -> Boolean) {
        senders[sessionId] = send
    }

    /** Sends [text] to the session as the user's next message; false when nothing there can take it now. */
    fun send(sessionId: String, text: String): Boolean =
        text.isNotBlank() && runCatching { senders[sessionId]?.invoke(text.trim()) }.getOrNull() == true

    fun forget(sessionId: String) {
        screen.remove(sessionId)
        transcripts.remove(sessionId)
        senders.remove(sessionId)
        changeCounts.value -= sessionId
        threadState.value -= sessionId
        publish()
    }

    private fun publish() {
        state.value = (screen.keys + transcripts.keys).mapNotNull { id -> resultFor(id)?.let { id to it } }.toMap()
    }
}
