package app.thdev.moru.core.terminal

import com.jediterm.terminal.model.TerminalTextBuffer

/**
 * The visible emulator screen's rows as plain text, top to bottom, for [screenQuestion] and [screenBusy]. The caller
 * holds the buffer's lock.
 */
fun terminalScreenRows(buffer: TerminalTextBuffer): List<String> =
    (0 until buffer.height).map { row ->
        buffer.getLine(row).text.filter { char -> !char.isISOControl() && char != '' }
    }
