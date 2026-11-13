package app.thdev.moru.feature.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionClock
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.TerminalSessionManager

/**
 * Per-Task session activity that follows the live terminals. Output reports do not bump
 * the manager's revision, so this is re-read on the shared [SessionClock], like the Sessions tab.
 */
@Composable
fun rememberTaskSessionActivities(sessionManager: TerminalSessionManager): Map<String, SessionActivity> {
    val nowMillis = rememberSessionNow()
    val responseRevision by SessionResponseAttention.revision.collectAsState()
    responseRevision
    return taskSessionActivities(sessionManager.listSessions(), nowMillis)
}
