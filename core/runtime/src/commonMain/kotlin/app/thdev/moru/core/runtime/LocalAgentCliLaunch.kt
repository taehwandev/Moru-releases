package app.thdev.moru.core.runtime

/** A fixed shell launch and, when needed, a command the user runs in the native CLI after startup. */
data class LocalAgentCliLaunch(val startupCommand: String, val slashCommand: String? = null)
