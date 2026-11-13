package app.thdev.moru.core.terminal

/**
 * The question an agent ended its last message with, or null when that message does not ask anything. An agent that
 * finishes its turn by asking ("Which color do you prefer?") waits for the user's reply, unlike one that only reports.
 * Claude starts a message with "⏺", Codex with "•", Gemini/AGY with "✦"; the agent's input line ("❯", "›", ">") sits
 * below it, and a footer such as "✻ Churned for 2s" or "Worked for 3s" may sit between. [rows] are the visible screen
 * rows, top to bottom. Read only a finished turn this way: while the agent works, its last message is not its last word.
 */
fun screenReplyQuestion(rows: List<String>): String? {
    val message = screenLastMessage(rows) ?: return null
    val last = message.lastOrNull { row -> row.isNotEmpty() }?.trim() ?: return null
    if (!last.endsWith("?") && !last.endsWith("？")) return null
    return if (last.length > ReplyLimit) last.take(ReplyLimit - 1) + "…" else last
}

/** The rows of the agent's last message on screen, its start mark removed, or null when none is visible. */
internal fun screenLastMessage(rows: List<String>): List<String>? {
    val visible = rows.map { row -> row.trim { char -> char.isWhitespace() || char in ReplyBorderChars } }
    val input = visible.indexOfLast { row -> InputPrompt.matches(row) }.takeIf { it >= 0 } ?: visible.size
    val start = visible.subList(0, input).indexOfLast { row -> MessageStart.containsMatchIn(row) }
    if (start < 0) return null
    val message = mutableListOf(visible[start].replaceFirst(MessageStart, ""))
    for (row in visible.subList(start + 1, input)) {
        if (TurnFooter.containsMatchIn(row)) break
        message += row
    }
    return message
}

private const val ReplyLimit = 160
private const val ReplyBorderChars = "│┃║"
private val MessageStart = Regex("^[⏺•✦]\\s+")
private val InputPrompt = Regex("^[❯›>](\\s.*)?$")

/** What follows a turn: a spinner line ("✻ Churned for 2s · done"), "Worked for 3s • 1:59 PM", or the input box rule. */
private val TurnFooter = Regex("^([✻✶✳✢✽]\\s|Worked for\\b|[─━═]{3,})")
