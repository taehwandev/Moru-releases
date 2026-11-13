package app.thdev.moru.core.terminal

/** Fixed menu movement from the currently marked choice. Labels never become executable input. */
fun permissionInput(prompt: SessionPermissionPrompt, choiceId: String): String? {
    if (prompt.choices.none { it.id == choiceId }) return null
    if (prompt.yesNo) return choiceId.takeIf { it == "y" || it == "n" }?.plus("\r")
    val from = prompt.choices.indexOfFirst { it.id == prompt.selectedChoiceId }
    val to = prompt.choices.indexOfFirst { it.id == choiceId }
    if (from < 0 || to < 0) return null
    val arrow = if (to > from) "\u001b[B" else "\u001b[A"
    return arrow.repeat(kotlin.math.abs(to - from)) + "\r"
}
