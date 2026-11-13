package app.thdev.moru.feature.terminal

import com.jediterm.terminal.model.SelectionUtil
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TerminalScrollbackClipboardTest {
    @Test
    fun aMouseReportingCliStillAllowsCopyingRealScrollback() {
        PreeditTestFixture().use { fixture ->
            fixture.output((1..35).joinToString("") { "history-$it\r\n" } + "\u001B[?1003h")
            waitForHistory(fixture)
            onEdt {
                val panel = fixture.panel
                panel.dispatchEvent(wheel(panel, -3))
                val origin = panel.verticalScrollModel.value
                assertTrue(origin < 0)
                val expected = panel.terminalTextBuffer.getLine(origin).text
                assertTrue(expected.startsWith("history-"))
                val width = fixture.cellWidth
                val height = panel.getFontMetrics(fixture.settings.terminalFont).height
                panel.dispatchEvent(mouse(panel, MouseEvent.MOUSE_PRESSED, 4, height / 2, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK))
                panel.dispatchEvent(mouse(panel, MouseEvent.MOUSE_DRAGGED, 4 + expected.length * width, height / 2, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK))
                panel.dispatchEvent(mouse(panel, MouseEvent.MOUSE_RELEASED, 4 + expected.length * width, height / 2, MouseEvent.BUTTON1, 0))
                val selection = assertNotNull(panel.selection)
                assertEquals(expected, SelectionUtil.getSelectionText(selection, panel.terminalTextBuffer))
                panel.dispatchEvent(wheel(panel, -1))
                assertNotNull(panel.selection)
            }
            assertTrue(!fixture.connector.hasWriteWithin(100), "Selection and scrolling must not send input to the CLI")
        }
    }

    @Test
    fun commandVPastesMultipleLinesWithBracketedPasteMarkers() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = clipboard.getContents(null)
        try {
            clipboard.setContents(StringSelection("first\nsecond"), null)
            PreeditTestFixture().use { fixture ->
                fixture.output("\u001B[?2004h")
                onEdt {
                    fixture.panel.processKeyEvent(KeyEvent(fixture.panel, KeyEvent.KEY_PRESSED, 0,
                        InputEvent.META_DOWN_MASK, KeyEvent.VK_V, 'v'))
                }
                assertTrue(fixture.connector.awaitText("\u001B[200~first\rsecond\u001B[201~"))
            }
        } finally {
            if (previous != null) clipboard.setContents(previous, null)
        }
    }

    @Test
    fun altScrollStillReachesTheMouseReportingCli() {
        PreeditTestFixture().use { fixture ->
            fixture.output("\u001B[?1003h")
            onEdt { fixture.panel.dispatchEvent(wheel(fixture.panel, -1, InputEvent.ALT_DOWN_MASK)) }
            assertTrue(fixture.connector.hasWriteWithin(500), "Alt+scroll must remain available to the CLI")
        }
    }

    @Test
    fun alternateScreenScrollStillReachesTheMouseReportingCli() {
        PreeditTestFixture().use { fixture ->
            fixture.output("\u001B[?1049h\u001B[?1003h")
            onEdt { fixture.panel.dispatchEvent(wheel(fixture.panel, -1)) }
            assertTrue(fixture.connector.hasWriteWithin(500), "A fullscreen TUI must retain its scrolling")
        }
    }

    private fun waitForHistory(fixture: PreeditTestFixture) {
        repeat(100) {
            if (onEdt { fixture.panel.verticalScrollModel.minimum < 0 }) return
            Thread.sleep(10)
        }
        error("Real terminal scrollback did not become available")
    }

    private fun wheel(panel: java.awt.Component, rotation: Int, modifiers: Int = 0) = MouseWheelEvent(panel, MouseEvent.MOUSE_WHEEL, 0,
        modifiers, 10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation)

    private fun mouse(panel: java.awt.Component, id: Int, x: Int, y: Int, button: Int, modifiers: Int) =
        MouseEvent(panel, id, 0, modifiers, x, y, 1, false, button)
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
