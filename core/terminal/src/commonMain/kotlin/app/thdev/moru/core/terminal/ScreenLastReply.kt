package app.thdev.moru.core.terminal

/**
 * The agent's last message as the screen shows it, its lines joined and blank runs collapsed, or null when none is
 * visible. It is what a finished turn reports, read the way [screenReplyQuestion] reads its question; an agent whose
 * own record holds the whole message ([SessionResults.recordTranscript]) is read from there instead.
 */
fun screenLastReply(rows: List<String>): String? {
    val message = screenLastMessage(rows) ?: return null
    val text = message.joinToString("\n").replace(BlankRun, "\n\n").trim()
    return text.takeIf(String::isNotEmpty)
}

private val BlankRun = Regex("\n{3,}")
