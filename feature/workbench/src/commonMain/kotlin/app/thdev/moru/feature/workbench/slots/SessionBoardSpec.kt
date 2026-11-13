package app.thdev.moru.feature.workbench.slots

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef

/** The sidebar's status board: the rows, where each Task sits ([tasks] by Task key), and what choosing a session or its page does. */
data class SessionBoardSpec(
    val rows: List<AgentSessionRow>,
    val tasks: Map<String, SessionTaskRef>,
    val onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    val onOpenPreview: (sessionId: String, url: String) -> Unit,
)
