package app.thdev.moru.app

/** What the Tasks need from the user right now, whether any terminal is still busy, and which agents are at work. */
internal data class AttentionState(
    val candidates: List<AttentionCandidate> = emptyList(),
    val anyWorking: Boolean = false,
    val working: List<WorkingAgent> = emptyList(),
)

/** An agent busy in a Task's terminal. */
internal data class WorkingAgent(val taskId: String, val sessionId: String, val title: String, val agent: String)
