package app.thdev.moru.core.terminal

/**
 * Reads [question] and [details] as Claude, Codex, Gemini and Lab print them ("Bash command" over the command,
 * "$ cmd" under Codex's question, "Allow execution of: 'npm install'?", "Do you want to make this edit to Main.kt?").
 * A command is kept whole, every line of it, since it is what the user approves; Claude's one-line description under
 * it is left to the details.
 */
fun permissionSummary(question: String, details: String): PermissionSummary {
    val rows = details.lines().map(String::trim).filter(String::isNotEmpty)
    val headerIndex = rows.indexOfLast { row -> Headers.any { (header, _) -> header.matches(row) } }
    val header = rows.getOrNull(headerIndex)
    val kind = header?.let { row -> Headers.first { (pattern, _) -> pattern.matches(row) }.second }
        ?: QuestionKinds.firstOrNull { (pattern, _) -> pattern.containsMatchIn(question) }?.second
        ?: if (rows.any { it.startsWith("$ ") }) PermissionKind.Command else PermissionKind.Other
    val dollar = rows.indexOfLast { it.startsWith("$ ") }
    val target = rows.takeIf { dollar >= 0 }?.drop(dollar)?.joinToString("\n")?.removePrefix("$ ")
        ?: header?.let { row -> HeaderTarget.find(row)?.groupValues?.get(1) ?: headerTarget(rows.drop(headerIndex + 1), kind) }
        ?: Quoted.find(question)?.groupValues?.get(1)
        ?: EditTo.find(question)?.groupValues?.get(1)
        ?: rows.takeIf { LabQuestion.matches(question) }?.let { lab -> if (kind == PermissionKind.Command) lab.joinToString("\n") else lab.firstOrNull() }
    return PermissionSummary(kind, target?.trim()?.takeIf(String::isNotEmpty))
}

/** Under Claude's "Bash command" every row is the command except the description it ends with; elsewhere one row. */
private fun headerTarget(below: List<String>, kind: PermissionKind): String? = when {
    kind != PermissionKind.Command -> below.firstOrNull()
    below.any { Separator.matches(it) } -> below.dropWhile { !Separator.matches(it) }.dropWhile { Separator.matches(it) }
        .takeWhile { !Separator.matches(it) && !it.startsWith("Hook PreToolUse:") }.joinToString("\n").trim().takeIf(String::isNotEmpty)
    below.size >= 2 -> below.dropLast(1).joinToString("\n")
    else -> below.firstOrNull()
}
private val Separator = Regex("^[─━═-]{3,}$")
private val Headers = listOf(
    Regex("(?i)^(bash|shell) command$") to PermissionKind.Command,
    Regex("(?i)^(edit|update) (file\\b.*|\\S+\\.\\S+)$") to PermissionKind.Edit,
    Regex("(?i)^(create|write) (file\\b.*|\\S+\\.\\S+)$") to PermissionKind.Create,
    Regex("(?i)^read (file\\b.*|\\S+\\.\\S+)$") to PermissionKind.Read,
    Regex("(?i)^(fetch|web search)$") to PermissionKind.Web,
    Regex("(?i)^tool use$") to PermissionKind.Tool,
)
private val HeaderTarget = Regex("(?i)^(?:edit|update|create|write|read) (?!file\\b)(\\S+\\.\\S+)$")
private val QuestionKinds = listOf(
    Regex("(?i)run the following command|execution of|^allow (bash|shell)\\b") to PermissionKind.Command,
    Regex("(?i)make (this|the following) edits?|^allow (edit|multiedit)\\b") to PermissionKind.Edit,
    Regex("(?i)\\bcreate\\b|^allow write\\b") to PermissionKind.Create,
    Regex("(?i)\\bfetch\\b|^allow (webfetch|websearch)\\b") to PermissionKind.Web,
    Regex("(?i)^allow (read|glob|grep)\\b") to PermissionKind.Read,
)
private val Quoted = Regex("['`\"]([^'`\"]+)['`\"]")
private val EditTo = Regex("(?i)\\b(?:edit to|create) (\\S+\\.\\S+?)\\?")
private val LabQuestion = Regex("^Allow \\S+\\?$")
