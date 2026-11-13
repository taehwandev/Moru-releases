package app.thdev.moru.core.terminal

/**
 * Follows a stream of terminal output and keeps the newest line that says something, so a session can
 * show what its program last printed without any program cooperating. Colour and cursor codes are
 * dropped, a bare carriage return starts a fresh line (a progress line redraws itself), and a line with
 * no letter or digit in it (a box border, a spinner glyph) is skipped. The unfinished line counts, since
 * a prompt such as "Proceed? (y/n)" never ends in a newline.
 */
class LastOutputLineTracker {
    private val pending = StringBuilder()
    private var lastFinished: String? = null

    /** Feeds one chunk and returns the newest meaningful line so far, or null if there has been none. */
    fun accept(chunk: String): String? {
        for (char in chunk.replace(RowMove, "\n").replace(ColumnMove, " ")) {
            if (char == '\n' || char == '\r') {
                clean(pending.toString())?.let { line -> lastFinished = line }
                pending.setLength(0)
            } else {
                pending.append(char)
            }
        }
        if (pending.length > MaxPendingLength) pending.delete(0, pending.length - MaxPendingLength)
        return clean(pending.toString()) ?: lastFinished
    }

    private fun clean(raw: String): String? {
        val text = ControlSequence.replace(raw, "")
            .map { char -> if (char == '\t') ' ' else char }
            .filter { char -> !char.isISOControl() }
            .joinToString("")
            .trim { char -> char.isWhitespace() || char in BorderChars }
            .replace(Whitespace, " ")
        if (text.none { char -> char.isLetterOrDigit() }) return null
        return if (text.length > MaxLineLength) text.take(MaxLineLength - 1) + "…" else text
    }

    private companion object {
        const val MaxLineLength = 200
        const val MaxPendingLength = 2_000
        const val BorderChars = "│┃║╎┆|─━═╭╮╰╯┌┐└┘"
        val Whitespace = Regex("\\s{2,}")
        val RowMove = Regex("\u001B\\[[0-9;]*[BEFHd]")
        val ColumnMove = Regex("\u001B\\[[0-9;]*[CG]")
        val ControlSequence = Regex("\u001B\\[[0-?]*[ -/]*[@-~]|\u001B\\][^\u0007]*\u0007|\u001B[()][A-Za-z0-9]")
    }
}
