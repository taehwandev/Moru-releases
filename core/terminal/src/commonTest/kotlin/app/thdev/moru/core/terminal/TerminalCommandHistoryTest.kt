package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalCommandHistoryTest {
    @Test
    fun parserCapturesCompletedCommandsAndHandlesBackspace() {
        val parser = TerminalCommandInputParser()

        val first = parser.accept("printff\b 'ok'\r")
        val second = parser.accept("pwd\n")

        assertEquals(listOf("printf 'ok'"), first)
        assertEquals(listOf("pwd"), second)
    }

    @Test
    fun parserIgnoresEscapeSequencesAndBlankLines() {
        val parser = TerminalCommandInputParser()

        val commands = parser.accept("\u001B[A\n  \nls -la\r")

        assertEquals(listOf("ls -la"), commands)
    }

    @Test
    fun onlyAnEnterOutsideBracketedPasteSignalsSubmittedInput() {
        val parser = TerminalCommandInputParser()
        var submissions = 0
        fun accept(text: String) = parser.accept(text) { submissions += 1 }
        accept("draft text\u001B[I\u001B[O\u001B[A")
        assertEquals(0, submissions)
        // Bracketed-paste markers may be split between connector writes.
        accept("\u001B[20")
        accept("0~pasted\nmultiple\rparagraphs\u001B[201~")
        assertEquals(0, submissions)
        accept("\r")
        assertEquals(1, submissions)
        // Empty Enter can submit a prefilled CLI prompt or a permission choice.
        accept("\n")
        assertEquals(2, submissions)
    }
}
