package app.thdev.moru.core.model

/**
 * Bounded descriptors for a Task's runtime children, selected views, and
 * recovery state. A Work Context id stays separate from the Task id so a future
 * decision can replace or fork a context without changing Task identity. A
 * descriptor never asserts that an OS process is still alive.
 *
 * V1 creates at most one Work Context per Task, and only when execution or
 * advanced inspection needs it.
 */
data class WorkContextDescriptor(
    val id: String,
    val taskId: String,
    val persistedLifecycle: WorkContextPersistedLifecycle = WorkContextPersistedLifecycle.NotCreated,
    val branch: String? = null,
    val worktreePath: String? = null,
    val terminalSessions: List<TerminalSessionDescriptor> = emptyList(),
    val agentSessions: List<AgentSessionDescriptor> = emptyList(),
)
