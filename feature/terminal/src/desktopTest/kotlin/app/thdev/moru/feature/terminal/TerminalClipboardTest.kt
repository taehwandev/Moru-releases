package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.darkCkColors
import com.jediterm.terminal.TerminalOutputStream
import com.jediterm.terminal.emulator.mouse.MouseMode
import com.jediterm.terminal.model.JediTerminal
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.InputMethodEvent
import java.awt.font.TextHitInfo
import java.text.AttributedString
import javax.swing.JScrollBar
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TerminalClipboardTest {
    @Test
    fun plainDragSelectsTextWhileTheCliReportsMouseInput(): Unit = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.dragSelection()
            assertNotNull(fixture.panel.selection, "Codex mouse reporting must still allow local text selection")
        }
    }

    @Test
    fun commandCCopiesTheSelectionWithoutClearingIt(): Unit = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.dragSelection()
            val event = fixture.commandC()
            fixture.panel.processKeyEvent(event)
            assertEquals(listOf("hello"), fixture.copied)
            assertTrue(event.isConsumed)
            assertNotNull(fixture.panel.selection)
        }
    }

    @Test
    fun rightClickCopiesOnceWithoutSendingTheClickToTheCli() = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.dragSelection()
            fixture.remoteInput.clear()
            listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED).forEach { id ->
                val event = fixture.mouse(id, MouseEvent.BUTTON3, InputEvent.BUTTON3_DOWN_MASK)
                fixture.panel.dispatchEvent(event)
                assertTrue(event.isConsumed)
            }
            assertEquals(listOf("hello"), fixture.copied)
            assertTrue(fixture.remoteInput.isEmpty())
        }
    }

    @Test
    fun commandCWithoutASelectionDoesNotReplaceTheClipboard() = onEdt {
        ClipboardFixture().use { fixture ->
            val event = fixture.commandC()
            fixture.panel.processKeyEvent(event)
            assertTrue(fixture.copied.isEmpty())
            assertTrue(event.isConsumed)
        }
    }

    @Test
    fun commandCCopiesKoreanTextWithoutLosingWideCharacters() = onEdt {
        ClipboardFixture("한글 hello").use { fixture ->
            fixture.dragSelection(9)
            fixture.panel.processKeyEvent(fixture.commandC())
            assertEquals(listOf("한글 hello"), fixture.copied)
        }
    }

    @Test
    fun copyingDoesNotCancelAnActiveKoreanComposition() = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.dragSelection()
            fixture.panel.dispatchEvent(InputMethodEvent(
                fixture.panel, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                AttributedString("한").iterator, 0, TextHitInfo.leading(1), null,
            ))
            fixture.panel.processKeyEvent(fixture.commandC())
            assertEquals(listOf("hello"), fixture.copied)
            val enter = KeyEvent(fixture.panel, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER, '\n')
            fixture.panel.processKeyEvent(enter)
            assertTrue(enter.isConsumed, "Enter must remain with the active IME composition")
        }
    }

    @Test
    fun ordinaryClicksStillReachTheCliButSelectionDragsDoNot() = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.panel.dispatchEvent(fixture.mouse(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK))
            fixture.panel.dispatchEvent(fixture.mouse(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 0))
            assertTrue(fixture.remoteInput.isNotEmpty())
            fixture.remoteInput.clear()
            fixture.panel.dispatchEvent(fixture.mouse(MouseEvent.MOUSE_DRAGGED, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK, 5))
            assertTrue(fixture.remoteInput.isEmpty())
        }
    }

    @Test
    fun aCompleteSelectionDragNeverReachesTheMouseReportingCli() = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.dragSelection()
            assertTrue(fixture.remoteInput.isEmpty(), "Even the initial press/release must stay local during a selection drag")
            fixture.panel.processKeyEvent(fixture.commandC())
            assertEquals(listOf("hello"), fixture.copied)
        }
    }

    @Test
    fun scrollingHistoryKeepsASelectionAndDoesNotSendMouseInput() = onEdt {
        ClipboardFixture().use { fixture ->
            fixture.dragSelection()
            fixture.remoteInput.clear()
            fixture.panel.verticalScrollModel.setRangeProperties(0, 10, -30, 10, false)
            val wheel = MouseWheelEvent(fixture.panel, MouseEvent.MOUSE_WHEEL, 0, 0, 10, 10, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1)
            fixture.panel.dispatchEvent(wheel)
            assertTrue(fixture.panel.verticalScrollModel.value < 0, "Wheel must scroll local history even with CLI mouse reporting")
            assertNotNull(fixture.panel.selection, "Scrolling must preserve the selected text")
            assertTrue(fixture.remoteInput.isEmpty())
        }
    }

    @Test
    fun controlCStillSendsTheInterruptCharacter() {
        PreeditTestFixture().use { fixture ->
            onEdt {
                fixture.panel.processKeyEvent(KeyEvent(fixture.panel, KeyEvent.KEY_PRESSED, 0, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_C, '\u0003'))
            }
            assertTrue(fixture.connector.awaitText("\u0003"))
        }
    }
}

private class ClipboardFixture(text: String = "hello") : AutoCloseable {
    val copied = mutableListOf<String>()
    val remoteInput = mutableListOf<String>()
    private val style = StyleState()
    private val buffer = TerminalTextBuffer(80, 10, style)
    private val settings = MoruTerminalSettingsProvider(null, 13, 1_000, TerminalPalette.Holder(TerminalPalette.from(darkCkColors)))
    val panel = MoruTerminalPanel(settings, buffer, style, copyToClipboard = { copied += it }).apply {
        init(JScrollBar())
        size = Dimension(640, 180)
    }
    private val terminal = JediTerminal(panel, buffer, style).apply {
        setTerminalOutput(object : TerminalOutputStream {
            override fun sendBytes(bytes: ByteArray, userInput: Boolean) { remoteInput += bytes.toString(Charsets.UTF_8) }
            override fun sendString(text: String, userInput: Boolean) { remoteInput += text }
        })
        setMouseMode(MouseMode.MOUSE_REPORTING_ALL_MOTION)
        writeCharacters(text)
    }

    init {
        panel.setCoordAccessor(terminal)
        panel.addTerminalMouseListener(terminal)
    }

    fun commandC() = KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, InputEvent.META_DOWN_MASK, KeyEvent.VK_C, 'c')

    /** Releases over the last selected cell, as a user does, so it ends on "o" of hello by default. */
    fun dragSelection(endColumn: Int = 4) {
        panel.dispatchEvent(mouse(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK))
        panel.dispatchEvent(mouse(MouseEvent.MOUSE_DRAGGED, MouseEvent.NOBUTTON, InputEvent.BUTTON1_DOWN_MASK, endColumn))
        panel.dispatchEvent(mouse(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 0, endColumn))
    }

    fun mouse(id: Int, button: Int, modifiers: Int, column: Int = 0): MouseEvent {
        val metrics = panel.getFontMetrics(settings.terminalFont)
        return MouseEvent(panel, id, 0, modifiers, 4 + column * metrics.charWidth('W'), metrics.height / 2, 1, button == MouseEvent.BUTTON3, button)
    }

    override fun close() = panel.dispose()
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
