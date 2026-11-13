package app.thdev.moru.feature.terminal

import com.jediterm.core.compatibility.Point
import com.jediterm.terminal.model.SelectionUtil
import com.jediterm.terminal.model.TerminalSelection
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** A selection dragged inside Claude Code, Codex or AGY survives the scrolling those CLIs do while they redraw. */
class TerminalSelectionScrollTest {
    @Test
    fun aSelectionInAFullScreenAgentStaysOnItsTextWhenTheScreenScrolls() {
        // Claude Code's and Codex's startup modes: alternate screen, any-motion mouse reporting, bracketed paste.
        assertSelectionFollowsScroll("\u001B[?1049h\u001B[?1000h\u001B[?1002h\u001B[?1003h\u001B[?1006h\u001B[?2004h")
    }

    @Test
    fun aSelectionInTheMainScreenFollowsItsTextIntoHistory() {
        assertSelectionFollowsScroll("\u001B[?2004h")
    }

    @Test
    fun selectedRowsScrolledOutOfTheRegionDropTheSelection() {
        val selection = TerminalSelection(Point(0, 0), Point(5, 1))
        assertNull(selection.afterScroll(top = 1, size = 10, dy = -1, intoHistory = false))
        assertNull(selection.afterScroll(top = 2, size = 5, dy = -1, intoHistory = true), "A selection the scrolled region cuts through is gone")
        assertSame(selection, selection.afterScroll(top = 4, size = 5, dy = -1, intoHistory = false))
        val moved = selection.afterScroll(top = 1, size = 10, dy = -1, intoHistory = true)
        assertEquals(listOf(Point(0, -1), Point(5, 0)), listOf(moved?.start, moved?.end))
    }

    private fun assertSelectionFollowsScroll(modes: String) {
        PreeditTestFixture().use { fixture ->
            fixture.output(modes + "\u001B[H" + (1..9).joinToString("\r\n") { "line-%02d hello".format(it) })
            val before = fixture.connector.writtenText()
            edt {
                val panel = fixture.panel
                val cell = panel.pixelHeight / panel.terminalTextBuffer.height
                fun at(id: Int, column: Int, row: Int, button: Int, modifiers: Int) = MouseEvent(
                    panel, id, 0, modifiers, 4 + column * fixture.cellWidth, row * cell + cell / 2, 1, false, button,
                )
                panel.dispatchEvent(at(MouseEvent.MOUSE_PRESSED, 0, 4, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK))
                panel.dispatchEvent(at(MouseEvent.MOUSE_DRAGGED, 7, 5, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK))
                panel.dispatchEvent(at(MouseEvent.MOUSE_RELEASED, 7, 5, MouseEvent.BUTTON1, 0))
            }
            assertEquals("line-05 hello\nline-06", selectedText(fixture))
            // A redraw that ends on the bottom row scrolls the screen, as Claude Code's and AGY's do.
            fixture.output("\r\nline-10 hello\r\nline-11 hello")
            assertEquals("line-05 hello\nline-06", selectedText(fixture))
            assertEquals(before, fixture.connector.writtenText(), "The drag must stay a local selection")
        }
    }

    private fun selectedText(fixture: PreeditTestFixture): String? = edt {
        fixture.panel.selection?.let { SelectionUtil.getSelectionText(it, fixture.panel.terminalTextBuffer) }
    }

    private fun <T> edt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }
}
