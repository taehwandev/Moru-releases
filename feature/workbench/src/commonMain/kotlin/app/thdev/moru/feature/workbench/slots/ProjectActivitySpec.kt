package app.thdev.moru.feature.workbench.slots

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef

/** A collapsed Project's observed sessions, where each Task sits ([tasks] by Task key), and what choosing one does. */
data class ProjectActivitySpec(
    val rows: List<AgentSessionRow>,
    val tasks: Map<String, SessionTaskRef>,
    val onSelectSession: (row: AgentSessionRow) -> Unit,
)
