package app.thdev.moru.feature.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalWindowInfo
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.terminal.ListeningServers
import app.thdev.moru.core.terminal.SESSION_CLOCK_TICK_MILLIS
import app.thdev.moru.core.terminal.LocalServerUrls
import app.thdev.moru.core.terminal.sessionPreviewUrl
import app.thdev.moru.core.terminal.SessionClock
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.SessionQuestions
import app.thdev.moru.core.terminal.SessionRunningAgents
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.SessionTitles
import app.thdev.moru.core.terminal.TerminalSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** A server that printed no address is found within this long; `lsof` runs only while a terminal runs a program. */
private const val SERVER_SCAN_INTERVAL_MILLIS = 10_000L

/**
 * Selection is read on each composition as well as the manager's bounded revision observer. A Task selection may
 * recompose before the next poll, so the selected session is a cache key of its own. Output reports do not bump
 * revision, so activity is re-read on the shared [SessionClock]. Server discovery lets a row offer to open its page.
 * [revision] is the manager's revision as the caller observes it, so the rows are re-read whenever it moves. Both scans read once and
 * then repeat only while the window has focus. [scans] is false for a second view of the same rows, such as a popup,
 * which reads what the always-present list already keeps up to date.
 */
@Composable
fun rememberAgentSessionRows(sessionManager: TerminalSessionManager, revision: Long, scans: Boolean = true): List<AgentSessionRow> {
    val nowMillis = rememberSessionNow()
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(sessionManager, scans, focused) {
        while (scans) {
            withContext(Dispatchers.Default) { ListeningServers.refresh() }
            if (!focused) break
            delay(SERVER_SCAN_INTERVAL_MILLIS)
        }
    }
    // Reading process trees runs no command, so it keeps pace with the clock the rows are re-read on.
    LaunchedEffect(sessionManager, scans, focused) {
        while (scans) {
            withContext(Dispatchers.Default) { SessionRunningAgents.refresh() }
            if (!focused) break
            delay(SESSION_CLOCK_TICK_MILLIS)
        }
    }
    val selectedSessionId = sessionManager.selectedSessionId()
    val responseRevision by SessionResponseAttention.revision.collectAsState()
    return remember(sessionManager, revision, nowMillis, selectedSessionId, responseRevision) {
        agentSessionRows(
            sessions = sessionManager.listSessions(),
            selectedSessionId = selectedSessionId,
            nowMillis = nowMillis,
            // A printed address keeps its path only while this terminal serves its port; otherwise its own server.
            previewUrlOf = { sessionId ->
                sessionPreviewUrl(
                    printed = LocalServerUrls.urlFor(sessionId),
                    servedPorts = ListeningServers.portsFor(sessionId),
                    served = ListeningServers.urlFor(sessionId),
                )
            },
            lastLineOf = SessionLastLines::lineFor,
            summaryOf = SessionTitles::titleFor,
            questionOf = SessionQuestions::questionFor,
            runningAgentOf = SessionRunningAgents::agentFor,
        )
    }
}
