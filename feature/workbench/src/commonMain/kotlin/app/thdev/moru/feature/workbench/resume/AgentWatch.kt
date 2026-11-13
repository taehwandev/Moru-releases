package app.thdev.moru.feature.workbench.resume

/**
 * What the Task's agent terminals are doing right now: the agents that quit while their terminal stayed open, whether
 * any agent is still running, and a way to clear one once the user resumed or dismissed it.
 */
class AgentWatch(
    val exits: List<AgentExit>,
    /**
     * Null until the first reading, while starting, or when a process cannot be observed. A Task must not show the resume bar then: it would
     * shrink the terminal and grow it back, and a running TUI such as Claude Code redraws broken for that resize.
     */
    val anyAgentRunning: Boolean?,
    val acknowledge: (sessionId: String) -> Unit,
)
