package app.thdev.moru.core.model

/**
 * The agent CLI ([program]: `claude`, `codex` or `agy`) running in a terminal right now and the conversation [id] it
 * holds, or null when the agent has not started one yet or keeps it where Moru cannot read it.
 */
data class AgentConversationRef(val program: String, val id: String?)
