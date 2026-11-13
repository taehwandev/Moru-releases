package app.thdev.moru.core.terminal

/** Observes OSC 11 background queries across PTY chunks without retaining terminal content. */
internal class TerminalBackgroundQueryTracker {
    private var matched = 0
    private var terminatorEscape = false

    fun accept(output: CharSequence): Boolean {
        var queried = false
        for (char in output) {
            if (accept(char)) queried = true
        }
        return queried
    }

    fun accept(char: Char): Boolean {
        var queried = false
        if (matched == Prefix.length) {
            if (char == '\u0007' || terminatorEscape && char == '\\') {
                queried = true
                matched = 0
                terminatorEscape = false
            } else if (!terminatorEscape && char == '\u001b') {
                terminatorEscape = true
            } else {
                matched = if (char == '\u001b') 1 else 0
                terminatorEscape = false
            }
        } else {
            matched = if (char == Prefix[matched]) matched + 1 else if (char == '\u001b') 1 else 0
        }
        return queried
    }

    private companion object {
        const val Prefix = "\u001b]11;?"
    }
}
