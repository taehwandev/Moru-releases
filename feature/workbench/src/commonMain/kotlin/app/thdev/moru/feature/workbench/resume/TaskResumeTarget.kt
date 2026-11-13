package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.feature.workbench.launch.AgentRuntime

/** A known conversation in the selected Task's latest terminal of this agent. */
internal data class TaskResumeTarget(
    val runtime: AgentRuntime,
    val command: String,
)
