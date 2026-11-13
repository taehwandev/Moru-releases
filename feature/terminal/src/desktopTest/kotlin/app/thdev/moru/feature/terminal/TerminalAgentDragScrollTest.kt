package app.thdev.moru.feature.terminal

import com.jediterm.terminal.model.SelectionUtil
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Claude Code and Codex draw on the alternate screen, which has no history: a selection dragged past the edge must ask
 * the CLI to scroll, and keep its anchor on the text the CLI then redraws further along.
 */
class TerminalAgentDragScrollTest {
    @Test
    fun aSelectionDraggedAboveTheTopAsksTheAgentToScrollUp() {
        PreeditTestFixture().use { fixture ->
            fixture.output(AgentModes + screen((1..8).map { "line-%02d hello".format(it) }))
            val before = fixture.connector.writtenText()
            onEdt {
                fixture.press(column = 0, row = 5)
                fixture.dragTo(column = 3, y = -fixture.rowHeight())
            }
            waitFor { fixture.connector.writtenText().removePrefix(before).split(WheelUpPrefix).size > 2 }
            onEdt { fixture.release(column = 3, row = 0) }
            assertTrue(fixture.connector.writtenText().removePrefix(before).startsWith(WheelUpPrefix))
        }
    }

    @Test
    fun theSelectionAnchorFollowsItsTextWhenTheAgentRedrawsItScrolled() {
        PreeditTestFixture().use { fixture ->
            val lines = (1..8).map { "line-%02d hello".format(it) }
            fixture.output(AgentModes + screen(lines + "> input"))
            onEdt {
                fixture.press(column = 0, row = 4)
                fixture.drag(column = 7, row = 5)
                fixture.panel.dispatchEvent(MouseWheelEvent(fixture.panel, MouseEvent.MOUSE_WHEEL, 0, Held, 4, 4, 4, 4, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1, -1.0))
            }
            // Codex's and Claude Code's answer to a wheel: the transcript redrawn two rows lower, the input box kept.
            fixture.output(screen(listOf("older-a", "older-b") + lines.take(6) + "> input"))
            onEdt {
                fixture.drag(column = 7, row = 7)
                fixture.release(column = 7, row = 7)
            }
            val selected = onEdt { SelectionUtil.getSelectionText(fixture.panel.selection!!, fixture.panel.terminalTextBuffer) }
            assertEquals("line-05 hello\nline-06", selected)
        }
    }

    @Test
    fun theShiftIsWhereMostRowsLineUpAgain() {
        val before = listOf("a", "b", "c", "d", "", "> input")
        assertEquals(2, screenShift(before, listOf("x", "y", "a", "b", "", "> input")))
        assertEquals(-1, screenShift(before, listOf("b", "c", "d", "e", "", "> input")))
        assertEquals(0, screenShift(before, before))
        assertEquals(0, screenShift(before, listOf("p", "q", "r", "s", "", "t")))
    }

    private fun PreeditTestFixture.rowHeight() = panel.pixelHeight / panel.terminalTextBuffer.height

    private fun PreeditTestFixture.pointer(id: Int, column: Int, y: Int, button: Int, modifiers: Int) =
        MouseEvent(panel, id, 0, modifiers, 4 + column * cellWidth, y, 1, false, button)

    private fun PreeditTestFixture.press(column: Int, row: Int) = panel.dispatchEvent(
        pointer(MouseEvent.MOUSE_PRESSED, column, row * rowHeight() + rowHeight() / 2, MouseEvent.BUTTON1, Held),
    )

    private fun PreeditTestFixture.drag(column: Int, row: Int) = dragTo(column, row * rowHeight() + rowHeight() / 2)

    private fun PreeditTestFixture.dragTo(column: Int, y: Int) =
        panel.dispatchEvent(pointer(MouseEvent.MOUSE_DRAGGED, column, y, MouseEvent.NOBUTTON, Held))

    private fun PreeditTestFixture.release(column: Int, row: Int) = panel.dispatchEvent(
        pointer(MouseEvent.MOUSE_RELEASED, column, row * rowHeight() + rowHeight() / 2, MouseEvent.BUTTON1, 0),
    )

    private fun screen(rows: List<String>) =
        rows.withIndex().joinToString("") { (row, text) -> "\u001B[${row + 1};1H\u001B[2K$text" }

    private fun waitFor(read: () -> Boolean) {
        val deadline = System.nanoTime() + 2_000_000_000L
        while (System.nanoTime() < deadline && !read()) Thread.sleep(20)
        assertTrue(read(), "The agent was not asked to scroll")
    }

    private fun <T> onEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }
}

// Claude Code's and Codex's startup modes: alternate screen, any-motion SGR mouse reporting, bracketed paste.
private const val AgentModes = "\u001B[?1049h\u001B[?1000h\u001B[?1002h\u001B[?1003h\u001B[?1006h\u001B[?2004h"
private const val WheelUpPrefix = "\u001B[<64;"
private const val Held = InputEvent.BUTTON1_DOWN_MASK
