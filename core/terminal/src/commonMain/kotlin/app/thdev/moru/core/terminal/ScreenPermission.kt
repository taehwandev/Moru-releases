package app.thdev.moru.core.terminal

/** Recognize permission choices, never model pickers or arbitrary questions. No output is used as executable input. */
fun screenPermission(rows: List<String>): SessionPermissionPrompt? {
    val visible = rows.map { it.trim { char -> char.isWhitespace() || char in PermissionBorders } }
        .filter(String::isNotEmpty).takeLast(24)
    val question = screenQuestion(rows) ?: return null
    if (!PermissionQuestion.containsMatchIn(question)) return null
    val last = visible.lastOrNull() ?: return null
    if (PermissionYesNo.containsMatchIn(last)) {
        if (!PermissionYesNoQuestion.matches(last)) return null
        return SessionPermissionPrompt(question, visible.dropLast(1).takeLast(8).joinToString("\n").take(1200),
            listOf(SessionPermissionChoice("y", "Yes"), SessionPermissionChoice("n", "No")), yesNo = true)
    }
    val start = visible.indexOfLast { it.take(159) == question.take(159) }
    if (start < 0) return null
    val menu = visible.drop(start + 1)
    val choices = menu.mapNotNull { row -> PermissionChoice.matchEntire(row)?.let { match ->
        SessionPermissionChoice(match.groupValues[2], match.groupValues[3])
    } }
    if (choices.size !in 2..6 || choices.map { it.id }.distinct().size != choices.size) return null
    if (choices.any { it.label.length > 240 || !PermissionAnswer.containsMatchIn(it.label) }) return null
    val first = menu.indexOfFirst { PermissionChoice.matches(it) }
    val lastChoice = menu.indexOfLast { PermissionChoice.matches(it) }
    if (menu.subList(first, lastChoice + 1).any { !PermissionChoice.matches(it) }) return null
    val selected = menu.firstNotNullOfOrNull { row -> PermissionChoice.matchEntire(row)?.takeIf { it.groupValues[1].isNotEmpty() }?.groupValues?.get(2) }
        ?: return null
    // Avoid a historical menu followed by a returned shell prompt or unrelated screen content.
    if (menu.dropWhile { PermissionChoice.matchEntire(it)?.groupValues?.get(2) != choices.last().id }
        .drop(1).any { !PermissionFooter.containsMatchIn(it) }) return null
    val firstChoice = start + 1 + menu.indexOfFirst { PermissionChoice.matches(it) }
    val details = visible.take(firstChoice).filter { it != question }.takeLast(12).joinToString("\n")
    if (details.length > 1200) return null
    return SessionPermissionPrompt(question, details, choices, selected)
}

private const val PermissionBorders = "│┃║╎┆|─━═╭╮╰╯┌┐└┘▌"
private val PermissionChoice = Regex("^([❯›>▶▸●]?)\\s*(\\d+)[.)]\\s+(\\S.*)$")
private val PermissionQuestion = Regex("\\b(allow|permission|approve|proceed|execute|trust|run|access|edit|write|create|delete|overwrite)\\b", RegexOption.IGNORE_CASE)
private val PermissionAnswer = Regex("^(yes|no|allow|deny|reject|cancel|approve|proceed)\\b", RegexOption.IGNORE_CASE)
private val PermissionYesNo = Regex("[(\\[]\\s*(y/n|yes/no)\\s*[)\\]]", RegexOption.IGNORE_CASE)
private val PermissionYesNoQuestion = Regex("^(allow|approve|permit|trust|overwrite|delete|create|write|execute|run|grant)\\b[^#$%>❯›]*\\?\\s*[(\\[]\\s*(y/n|yes/no)\\s*[)\\]]$", RegexOption.IGNORE_CASE)
private val PermissionFooter = Regex("(?i)^(press\\b|enter\\b|esc\\b|tab\\b|↑|↓|use (the )?arrow\\b)")
