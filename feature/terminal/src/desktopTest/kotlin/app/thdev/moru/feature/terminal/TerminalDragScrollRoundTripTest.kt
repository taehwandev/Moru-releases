package app.thdev.moru.feature.terminal

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The user's full native gesture: hold a selection, scroll history, copy it, then paste the selected lines. */
class TerminalDragScrollRoundTripTest {
    @Test
    fun scrollingAHeldSelectionCopiesAndPastesTheFullRangeThroughNativeEvents() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = clipboard.getContents(null)
        clipboard.setContents(StringSelection("no-selection-copied"), null)
        try {
            PreeditTestFixture().use { fixture ->
                fixture.output("\u001B[?2004h\u001B[?1003h" + (1..35).joinToString("") { "record-%02d\r\n".format(it) })
                waitFor { onEdt { fixture.panel.verticalScrollModel.minimum < 0 } }
                val expected = onEdt {
                    val panel = fixture.panel
                    val row = 2
                    val startRecord = recordAt(fixture, row)
                    val cellHeight = panel.pixelHeight / panel.terminalTextBuffer.height
                    val y = row * cellHeight + cellHeight / 2
                    fun pointer(id: Int, x: Int, button: Int, modifiers: Int) =
                        MouseEvent(panel, id, 0, modifiers, x, y, 1, false, button)
                    val held = InputEvent.BUTTON1_DOWN_MASK
                    panel.dispatchEvent(pointer(MouseEvent.MOUSE_PRESSED, 4 + 9 * fixture.cellWidth, MouseEvent.BUTTON1, held))
                    panel.dispatchEvent(pointer(MouseEvent.MOUSE_DRAGGED, 4, MouseEvent.NOBUTTON, held))
                    panel.dispatchEvent(MouseWheelEvent(panel, MouseEvent.MOUSE_WHEEL, 0, held, 4, y, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -3))
                    val endRecord = recordAt(fixture, panel.verticalScrollModel.value + row)
                    assertTrue(endRecord < startRecord)
                    panel.dispatchEvent(pointer(MouseEvent.MOUSE_RELEASED, 4, MouseEvent.BUTTON1, 0))
                    val selected = (endRecord..startRecord).joinToString("\n") { "record-%02d".format(it) }
                    panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, InputEvent.META_DOWN_MASK, KeyEvent.VK_C, 'c'))
                    assertEquals(selected, clipboard.getData(DataFlavor.stringFlavor), "Copy must cover the history reached while holding the selection")
                    panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_PRESSED, 1, InputEvent.META_DOWN_MASK, KeyEvent.VK_V, 'v'))
                    selected
                }
                assertTrue(fixture.connector.awaitText("\u001B[200~" + expected.replace('\n', '\r') + "\u001B[201~"))
            }
        } finally {
            if (previous != null) clipboard.setContents(previous, null)
        }
    }

    private fun recordAt(fixture: PreeditTestFixture, row: Int): Int =
        fixture.panel.terminalTextBuffer.getLine(row).text.removePrefix("record-").trim().toInt()

    private fun waitFor(message: String = "Native terminal condition timed out", read: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline && !read()) Thread.sleep(25)
        assertTrue(read(), message)
    }
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
