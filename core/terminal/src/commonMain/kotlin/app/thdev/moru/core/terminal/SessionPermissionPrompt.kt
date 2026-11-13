package app.thdev.moru.core.terminal

/** Bounded live observation; selection is included so a changed menu invalidates old input. */
data class SessionPermissionPrompt(
    val question: String,
    val details: String,
    val choices: List<SessionPermissionChoice>,
    val selectedChoiceId: String? = null,
    val yesNo: Boolean = false,
)
