package app.thdev.moru.core.terminal

import com.jediterm.terminal.model.TerminalTextBuffer

/** Projects the visible emulator screen, including cursor-addressed redraws, into one bounded line. */
fun terminalScreenSummary(buffer: TerminalTextBuffer): String? {
    // The emulator writes from its reader thread. Never hold up Swing while it is processing a frame.
    if (!buffer.tryLock()) return null
    return try {
        (buffer.height - 1 downTo 0).firstNotNullOfOrNull { row ->
            val text = buffer.getLine(row).text
                .filter { char -> !char.isISOControl() && char != '\uE000' }
                .trim { char -> char.isWhitespace() || char in ScreenBorderChars }
            if (text.none(Char::isLetterOrDigit) || isTerminalFooter(text)) null
            else if (text.length > SummaryLimit) text.take(SummaryLimit - 1) + "…" else text
        }
    } finally {
        buffer.unlock()
    }
}

private fun isTerminalFooter(text: String): Boolean =
    text.startsWith("? for shortcuts") ||
        text.startsWith("? for commands") ||
        text.contains("keyboard shortcuts") ||
        text.contains("twice to exit") ||
        text.contains("pending artifacts") ||
        text.contains("with /artifact") ||
        text.contains("non-read tools") ||
        text.contains("esc to interrupt") ||
        (text.contains("context left") && !text.startsWith("›")) ||
        text.startsWith("shift+tab to cycle") ||
        text.startsWith("bypass permissions on") ||
        (text.contains(" · ") && (text.contains("5h ") || text.contains("7d ") || text.contains("█") || text.contains("░")))

private const val SummaryLimit = 200
private const val ScreenBorderChars = "│┃║╎┆|─━═╭╮╰╯┌┐└┘"
