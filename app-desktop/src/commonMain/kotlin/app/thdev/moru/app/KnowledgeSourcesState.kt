package app.thdev.moru.app

import app.thdev.moru.core.model.AgentWorkflowSource

internal data class KnowledgeSourcesState(
    val loading: Boolean = true,
    val sources: List<AgentWorkflowSource> = emptyList(),
    val error: String? = null,
)
