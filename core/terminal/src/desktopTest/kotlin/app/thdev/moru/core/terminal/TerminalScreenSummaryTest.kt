package app.thdev.moru.core.terminal

import com.jediterm.terminal.model.CharBuffer
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalScreenSummaryTest {
    @Test
    fun newestVisibleActivitySurvivesCodexFooterAndEmptyPrompt() {
        val screen = screen()
        screen.writeString(0, 2, CharBuffer("Working on the change"))
        screen.writeString(0, 5, CharBuffer("› "))
        screen.writeString(0, 7, CharBuffer("? for shortcuts  100% context left"))
        assertEquals("Working on the change", terminalScreenSummary(screen))
        screen.eraseCharacters(0, 80, 1)
        screen.writeString(0, 2, CharBuffer("Checking tests"))
        assertEquals("Checking tests", terminalScreenSummary(screen))
    }

    @Test
    fun typedKoreanPromptIsVisibleAndWideCellMarkersAreRemoved() {
        val screen = screen()
        screen.writeString(0, 2, CharBuffer("Previous response"))
        screen.writeString(0, 5, CharBuffer("› 한\uE000글\uE000 입력"))
        assertEquals("› 한글 입력", terminalScreenSummary(screen))
    }

    @Test
    fun plainShellAndClaudeOutputStillAppear() {
        val screen = screen()
        screen.writeString(0, 2, CharBuffer("│ Reading the project │"))
        screen.writeString(0, 5, CharBuffer("bypass permissions on (shift+tab to cycle)"))
        assertEquals("Reading the project", terminalScreenSummary(screen))
    }

    @Test
    fun agyOutputSurvivesFootersAndStatusLine() {
        val screen = screen()
        screen.writeString(0, 1, CharBuffer("Checking git status"))
        screen.writeString(0, 4, CharBuffer("Press esc to interrupt generation."))
        screen.writeString(0, 5, CharBuffer("Press ? to see keyboard shortcuts."))
        screen.writeString(0, 6, CharBuffer("Press ctrl+c or ctrl+d twice to exit."))
        screen.writeString(0, 7, CharBuffer("Gemini 3.8 Flash · ~/git/Moru · 5h ████████ 100%"))
        assertEquals("Checking git status", terminalScreenSummary(screen))
    }

    @Test
    fun blankScreenDoesNotInventActivity() {
        assertNull(terminalScreenSummary(screen()))
    }

    @Test
    fun boundsTheSummaryWithoutRetainingScrollback() {
        val screen = TerminalTextBuffer(500, 8, StyleState(), 0)
        screen.writeString(0, 2, CharBuffer("x".repeat(450)))
        assertEquals(200, terminalScreenSummary(screen)!!.length)
        assertEquals('…', terminalScreenSummary(screen)!!.last())
    }

    private fun screen() = TerminalTextBuffer(80, 8, StyleState(), 0)
}
