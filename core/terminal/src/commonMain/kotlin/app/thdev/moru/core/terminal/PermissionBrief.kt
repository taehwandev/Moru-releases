package app.thdev.moru.core.terminal

/** Display only: the exact request and command remain in Details and own approval authority. */
fun permissionBrief(summary: PermissionSummary, details: String): String {
    val target = summary.target.orEmpty().replace(Regex("\\s+"), " ").trim()
    val action = if (summary.kind == PermissionKind.Command && RestoreCommand.containsMatchIn(target)) {
        val files = Regex("[A-Za-z_][A-Za-z0-9_-]*\\.[A-Za-z0-9]+(?=\\s|$)").findAll(target)
            .map { it.value }.toList()
        "Run git restore" + files.takeIf { it.isNotEmpty() }
            ?.joinToString(", ", prefix = "\n")?.take(120).orEmpty()
    } else target.take(160) + if (target.length > 160) "…" else ""
    val reason = if (details.contains("Hook PreToolUse:") && details.contains("main-checkout exception")) {
        "Main checkout exception requires confirmation."
    } else ""
    return listOf(action, reason).filter(String::isNotBlank).joinToString("\n")
}
private val RestoreCommand = Regex("^(?:[A-Za-z_][A-Za-z0-9_]*=[^\\s]+\\s+)*git(?:\\s+-C\\s+(?:'[^']*'|\"[^\"]*\"|\\S+))?\\s+restore\\s")
