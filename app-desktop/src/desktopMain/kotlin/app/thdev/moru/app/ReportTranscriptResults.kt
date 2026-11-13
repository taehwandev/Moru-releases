package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thdev.moru.core.data.agent.agentTranscriptReply
import app.thdev.moru.core.data.agent.agentTranscriptThread
import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.model.session.ConversationTurn
import app.thdev.moru.core.terminal.SessionResults
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Replaces a terminal's on-screen result with the whole message from the agent's own transcript, once per finished
 * turn the screen shows, and keeps the exchanges that led to it ([SessionResults.threads]). The transcript is checked
 * against the screen, so a record written a moment late is not taken for the turn before it. Nothing is asked of any
 * model; [readConversation] and the transcript are local files.
 */
@Composable
internal fun ReportTranscriptResults(
    sessionManager: TerminalSessionManager,
    readConversation: (String) -> AgentConversationRef? = ::readDesktopAgentConversation,
    readReply: (AgentConversationRef) -> String? = { agentTranscriptReply(it) },
    readThread: (AgentConversationRef) -> List<ConversationTurn> = { agentTranscriptThread(it) },
) {
    LaunchedEffect(sessionManager) {
        val attempted = mutableMapOf<String, String>()
        SessionResults.results.collect {
            sessionManager.listSessions().filter { it.spec.kind == TerminalSessionKind.TERMINAL }.forEach { session ->
                val screen = SessionResults.screenFor(session.id) ?: return@forEach
                if (attempted[session.id] == screen) return@forEach
                attempted[session.id] = screen
                launch {
                    for (wait in TranscriptWaitsMillis) {
                        delay(wait)
                        if (SessionResults.screenFor(session.id) != screen) return@launch
                        val conversation = withContext(Dispatchers.IO) { runCatching { readConversation(session.id) }.getOrNull() }
                        val text = withContext(Dispatchers.IO) { runCatching { conversation?.let(readReply) }.getOrNull() }
                        if (conversation != null && text != null && transcriptMatchesScreen(text, screen)) {
                            val thread = withContext(Dispatchers.IO) { runCatching { readThread(conversation) }.getOrNull().orEmpty() }
                            if (SessionResults.screenFor(session.id) == screen) {
                                SessionResults.recordTranscript(session.id, text)
                                SessionResults.recordThread(session.id, thread)
                            }
                            return@launch
                        }
                    }
                }
            }
        }
    }
}

/**
 * The screen draws the message the transcript holds when its opening or closing words, letters and digits only, appear
 * there. A message taller than the screen has scrolled its opening away, but its ending is still in view.
 */
internal fun transcriptMatchesScreen(transcript: String, screen: String): Boolean {
    val letters = screen.filter(Char::isLetterOrDigit)
    if (letters.isEmpty()) return false
    val whole = transcript.filter(Char::isLetterOrDigit)
    return letters.take(MatchChars) in whole || letters.takeLast(MatchChars) in whole
}

private val TranscriptWaitsMillis = listOf(800L, 3_000L)
private const val MatchChars = 24
