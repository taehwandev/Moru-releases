package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.feature.workbench.launch.AgentRuntime

/**
 * An agent that quit in a terminal that is still open: which terminal, which agent it was, and the line that reopens
 * that terminal's own conversation.
 */
data class AgentExit(
    val sessionId: String,
    val runtime: AgentRuntime,
    val resumeCommand: String? = null,
)
