package app.thdev.moru.app

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionResponseAttention

/** An explicit native focus or same-row selection is also an opening, without claiming to detect reading. */
internal fun acknowledgeViewedSessionResponse(
    rows: List<AgentSessionRow>,
    sessionId: String,
    taskId: String?,
    workVisible: Boolean,
    windowFocused: Boolean,
): Boolean {
    if (!workVisible || !windowFocused || taskId == null) return false
    val row = rows.firstOrNull { it.sessionId == sessionId && it.taskKey == taskId } ?: return false
    if (row.agent !is AgentRuntime.Named || row.activity != SessionActivity.Quiet) return false
    return SessionResponseAttention.acknowledge(sessionId)
}
