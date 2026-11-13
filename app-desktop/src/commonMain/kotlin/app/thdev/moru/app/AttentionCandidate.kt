package app.thdev.moru.app

/**
 * A Task that needs the user, and why; [agent] names the agent that asks or went quiet, [question] what it asks,
 * [summary] what that agent says it worked on, and [sessionId] the terminal it happened in, so opening it lands on that
 * terminal.
 */
internal data class AttentionCandidate(
    val taskId: String,
    val title: String,
    val kind: AttentionKind,
    val agent: String? = null,
    val question: String? = null,
    val sessionId: String? = null,
    val summary: String? = null,
    val permissionRequestId: String? = null,
)
