package app.thdev.moru.feature.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.SessionQuestions
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.SessionRunningAgents
import app.thdev.moru.core.terminal.SessionTitles
import app.thdev.moru.core.terminal.TerminalSessionManager

/**
 * Each session's row by id, for labels that sit beside the terminal: the sidebar's state and title on the shared
 * clock, without the sidebar's server scan. [revision] is the manager's revision as the caller observes it.
 */
@Composable
fun rememberSessionLabelRows(sessionManager: TerminalSessionManager, revision: Long): Map<String, AgentSessionRow> {
    val nowMillis = rememberSessionNow()
    val selectedSessionId = sessionManager.selectedSessionId()
    val responseRevision by SessionResponseAttention.revision.collectAsState()
    return remember(sessionManager, revision, nowMillis, selectedSessionId, responseRevision) {
        agentSessionRows(
            sessions = sessionManager.listSessions(),
            selectedSessionId = selectedSessionId,
            nowMillis = nowMillis,
            lastLineOf = SessionLastLines::lineFor,
            summaryOf = SessionTitles::titleFor,
            questionOf = SessionQuestions::questionFor,
            runningAgentOf = SessionRunningAgents::agentFor,
        ).associateBy { it.sessionId }
    }
}
