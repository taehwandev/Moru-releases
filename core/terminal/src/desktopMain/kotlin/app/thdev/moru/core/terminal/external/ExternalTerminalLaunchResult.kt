package app.thdev.moru.core.terminal.external

/** Whether an external terminal opened for a session, with the message to show either way. */
data class ExternalTerminalLaunchResult(
    val opened: Boolean,
    val message: String,
)
