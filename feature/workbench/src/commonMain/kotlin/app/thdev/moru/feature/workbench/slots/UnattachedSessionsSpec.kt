package app.thdev.moru.feature.workbench.slots

import app.thdev.moru.core.model.session.AgentSessionRow

/** The sessions that belong to no Task, listed below the Projects, and what choosing one or its page does. */
data class UnattachedSessionsSpec(
    val rows: List<AgentSessionRow>,
    val onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    val onOpenPreview: (sessionId: String, url: String) -> Unit,
)
