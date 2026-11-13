package app.thdev.moru.core.model.session

/**
 * One terminal session as the sidebar shows it. It holds facts, not words: the panel turns
 * [agent], [activity], the output time and [exitCode] into text in the app's language.
 */
data class AgentSessionRow(
    val sessionId: String,
    val title: String,
    val agent: AgentRuntime,
    val activity: SessionActivity,
    /** When the session last printed, or null if it never has. */
    val lastOutputAtMillis: Long?,
    /** The time the row was built, so "how long ago" is measured from one clock reading. */
    val nowMillis: Long,
    val exitCode: Int?,
    /** A local server address the session printed, or null; the panel offers to open it. */
    val previewUrl: String?,
    val selected: Boolean,
    /** The newest meaningful line the session printed, or null; it says what the program is doing right now. */
    val lastLine: String? = null,
    /** What the agent says it is working on (its terminal title), or null; the row leads with it. */
    val summary: String? = null,
    /** The Task the session is attached to, or null for a session that belongs to none. */
    val taskKey: String? = null,
    /** The question the session's screen asks now, or null; the row shows it while the session waits for an answer. */
    val question: String? = null,
    /** The question the agent ended its last message with, or null; with [finishedTurn] it waits for the user's reply. */
    val replyQuestion: String? = null,
) {
    /** An agent that stopped printing has finished its turn; a plain terminal that is quiet is still running. */
    val finishedTurn: Boolean
        get() = activity == SessionActivity.Quiet && agent is AgentRuntime.Named

    /** A finished turn that ended by asking the user something: the agent waits for their reply. */
    val awaitsReply: Boolean
        get() = finishedTurn && replyQuestion != null
}
