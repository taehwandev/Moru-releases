package app.thdev.moru.core.terminal

/**
 * The question a program on screen is waiting for the user to answer, or null when it is not asking. Agents ask the
 * same way: a question, then a numbered list with one choice marked ("❯ 1. Yes" in Claude, "› 1. Yes, proceed" in
 * Codex, "● 1. Allow once" in Gemini/AGY). A plain program asks with "(y/n)" on its last line. [rows] are the visible
 * screen rows, top to bottom; only the bottom of the screen counts, since a question scrolled above is answered.
 */
fun screenQuestion(rows: List<String>): String? {
    val visible = rows.map { row -> row.trim { char -> char.isWhitespace() || char in QuestionBorderChars } }
        .filter { row -> row.isNotEmpty() }
        .takeLast(QuestionRowLimit)
    val last = visible.lastOrNull() ?: return null
    if (YesNo.containsMatchIn(last)) return last.bounded()
    val marked = visible.indexOfLast { row -> MarkedChoice.matches(row) }
    if (marked < 0) return null
    val choices = visible.count { row -> MarkedChoice.matches(row) || PlainChoice.matches(row) }
    if (choices < 2) return null
    return visible.take(marked)
        .lastOrNull { row -> row.endsWith("?") && !PlainChoice.matches(row) && row.any(Char::isLetterOrDigit) }
        ?.bounded()
}

private fun String.bounded(): String = if (length > QuestionLimit) take(QuestionLimit - 1) + "…" else this

private const val QuestionRowLimit = 24
private const val QuestionLimit = 160
private const val QuestionBorderChars = "│┃║╎┆|─━═╭╮╰╯┌┐└┘▌"
private val MarkedChoice = Regex("^[❯›>▶▸●]\\s*\\d+[.)]\\s+\\S.*")
private val PlainChoice = Regex("^\\d+[.)]\\s+\\S.*")
private val YesNo = Regex("[(\\[]\\s*(y/n|yes/no)\\s*[)\\]]", RegexOption.IGNORE_CASE)
