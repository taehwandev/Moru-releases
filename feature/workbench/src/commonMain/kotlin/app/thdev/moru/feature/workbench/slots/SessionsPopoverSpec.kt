package app.thdev.moru.feature.workbench.slots

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef

/**
 * The list of every session as the sidebar header offers it: the rows, where each Task sits ([tasks] by Task key),
 * the open Project it can narrow to, and what choosing a session or its page does.
 */
data class SessionsPopoverSpec(
    val rows: List<AgentSessionRow>,
    val tasks: Map<String, SessionTaskRef>,
    val selectedProjectId: String?,
    val onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    val onOpenPreview: (sessionId: String, url: String) -> Unit,
)
