package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.darkCkColors
import com.jediterm.terminal.model.StyleState
import com.jediterm.terminal.model.TerminalTextBuffer
import java.awt.Component
import java.awt.DefaultKeyboardFocusManager
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.font.TextHitInfo
import java.text.AttributedString
import javax.swing.JScrollBar
import javax.swing.JTextField
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The last Korean syllable stays composing until the next key; leaving the terminal must commit it, not drop it. */
class TerminalCompositionCommitTest {
    @Test
    fun leavingTheTerminalWhileComposingEndsTheComposition() = onEdt {
        val previous = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val focus = MovableFocusManager()
        KeyboardFocusManager.setCurrentKeyboardFocusManager(focus)
        try {
            val ended = mutableListOf<MoruTerminalPanel>()
            val panel = panel { ended += it }
            focus.move(null, panel)
            panel.compose("요")
            focus.move(panel, JTextField())
            assertEquals(listOf(panel), ended, "The composing syllable must be committed before focus leaves")
            panel.dispose()
        } finally {
            KeyboardFocusManager.setCurrentKeyboardFocusManager(previous)
        }
    }

    @Test
    fun leavingTheTerminalWithoutACompositionLeavesTheInputMethodAlone() = onEdt {
        val previous = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val focus = MovableFocusManager()
        KeyboardFocusManager.setCurrentKeyboardFocusManager(focus)
        try {
            val ended = mutableListOf<MoruTerminalPanel>()
            val panel = panel { ended += it }
            focus.move(null, panel)
            panel.compose("요")
            panel.compose("") // the input method cleared the syllable
            focus.move(panel, JTextField())
            assertTrue(ended.isEmpty())
            panel.dispose()
        } finally {
            KeyboardFocusManager.setCurrentKeyboardFocusManager(previous)
        }
    }

    @Test
    fun clickingTheTerminalWhileComposingEndsTheComposition() = onEdt {
        val ended = mutableListOf<MoruTerminalPanel>()
        val panel = panel { ended += it }
        panel.compose("요")
        panel.dispatchEvent(MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1))
        assertEquals(listOf(panel), ended)
        panel.dispose()
    }

    /** What the input method sends once the composition ends: the syllable, committed, after focus already moved. */
    @Test
    fun theCommitThatEndsTheCompositionReachesTheCli() {
        PreeditTestFixture().use { fixture ->
            fixture.composition("안", committedCharacters = 1)
            fixture.composition("요")
            fixture.composition("요", committedCharacters = 1)
            assertTrue(fixture.connector.awaitText("안요"))
            assertEquals("안요", fixture.connector.writtenText())
        }
    }

    /**
     * Another pane or window took focus mid-syllable and the input method's commit never came back here. The stale
     * syllable kept the terminal composing, so every key after the user returned was swallowed.
     */
    @Test
    fun aCompositionLostWithFocusDoesNotSwallowKeysAfterReturning() {
        PreeditTestFixture().use { fixture ->
            fixture.composition("요")
            fixture.focus(FocusEvent.FOCUS_LOST)
            fixture.focus(FocusEvent.FOCUS_GAINED)
            fixture.keyPressed(KeyEvent.VK_ENTER)
            assertTrue(fixture.connector.awaitText("\r"), "Keys must reach the CLI once the terminal has focus again")
            assertEquals("\r", fixture.connector.writtenText(), "The abandoned syllable must not be sent late")
        }
    }

    private fun panel(endComposition: (MoruTerminalPanel) -> Unit): MoruTerminalPanel {
        val style = StyleState()
        val settings = MoruTerminalSettingsProvider(null, 13, 1_000, TerminalPalette.Holder(TerminalPalette.from(darkCkColors)))
        return MoruTerminalPanel(settings, TerminalTextBuffer(80, 10, style), style, endComposition = endComposition).apply {
            init(JScrollBar())
            size = Dimension(640, 180)
        }
    }

    private fun MoruTerminalPanel.compose(text: String, committed: Int = 0) = dispatchEvent(
        InputMethodEvent(this, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, AttributedString(text).iterator,
            committed, TextHitInfo.leading(text.length - committed), null),
    )

    private class MovableFocusManager : DefaultKeyboardFocusManager() {
        fun move(from: Component?, to: Component) {
            if (from != null) setGlobalFocusOwner(null)
            setGlobalFocusOwner(to)
        }
    }
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(block) }
    return result!!.getOrThrow()
}
