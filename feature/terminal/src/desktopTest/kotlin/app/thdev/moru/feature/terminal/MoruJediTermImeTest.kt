@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package app.thdev.moru.feature.terminal

import com.jediterm.core.util.TermSize
import app.thdev.moru.core.designsystem.theme.lightCkColors
import com.jediterm.terminal.Questioner
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.TerminalPanel
import java.awt.event.KeyEvent
import java.awt.event.InputMethodEvent
import java.awt.font.TextHitInfo
import java.nio.charset.StandardCharsets
import java.text.AttributedString
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MoruJediTermImeTest {
    @Test
    fun committingOneKoreanSyllableKeepsTheNextSyllableInComposition() {
        val connector = RecordingTtyConnector()
        val widget = imeWidget().apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
        }
        widget.start()
        try {
            val panel = widget.terminalPanel
            SwingUtilities.invokeAndWait {
                panel.dispatchEvent(inputMethodEvent(panel, text = "한ㄱ", committedCharacterCount = 1))
            }
            assertTrue(connector.awaitWrite("한"))
            SwingUtilities.invokeAndWait { panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_ENTER)) }
            Thread.sleep(100)
            assertEquals("한", connector.writtenText(), "Enter must stay with the still-composing second syllable.")
        } finally {
            widget.close()
        }
    }

    @Test
    fun koreanInputMethodCommitWritesCommittedTextToTerminal() {
        val connector = RecordingTtyConnector()
        val widget = imeWidget().apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
        }
        widget.start()

        try {
            val panel = widget.terminalPanel
            panel.dispatchEvent(inputMethodEvent(panel, text = "한글", committedCharacterCount = 2))

            assertTrue(connector.awaitWrite("한글"), "Committed Korean IME text should be sent to the PTY connector.")
        } finally {
            widget.close()
        }
    }

    @Test
    fun uncommittedKoreanCompositionKeepsEnterAndBackspaceOutOfTerminal() {
        val connector = RecordingTtyConnector()
        val widget = imeWidget().apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
        }
        widget.start()

        try {
            val panel = widget.terminalPanel
            panel.dispatchEvent(inputMethodEvent(panel, text = "하", committedCharacterCount = 0))
            panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_BACK_SPACE))
            panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_ENTER))

            assertTrue(connector.awaitWriteCount(0), "Composition Backspace/Enter should stay with IME until text is committed.")
            assertNotNull(panel.inputMethodRequests, "Terminal panel should expose IME candidate-window location requests.")
        } finally {
            widget.close()
        }
    }

    private fun inputMethodEvent(
        panel: TerminalPanel,
        text: String,
        committedCharacterCount: Int,
    ): InputMethodEvent {
        return InputMethodEvent(
            panel,
            InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
            AttributedString(text).iterator,
            committedCharacterCount,
            TextHitInfo.leading(0),
            null,
        )
    }

    private fun imeWidget(): JediTermWidget = MoruJediTermWidget(
        80, 24, MoruTerminalSettingsProvider(
            null, 13, 1000, TerminalPalette.Holder(TerminalPalette.from(lightCkColors)),
        ),
    )

    @Test
    fun finishingMixedCompositionSendsBothSyllablesExactlyOnceAndAllowsEnter() {
        val connector = RecordingTtyConnector()
        val widget = imeWidget().apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
        }
        widget.start()
        try {
            val panel = widget.terminalPanel
            SwingUtilities.invokeAndWait {
                panel.dispatchEvent(inputMethodEvent(panel, "한ㄱ", 1))
                panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_BACK_SPACE))
                panel.dispatchEvent(inputMethodEvent(panel, "글", 1))
                panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_ENTER))
            }
            assertTrue(connector.awaitWrite("한글\r"))
            assertEquals("한글\r", connector.writtenText())
        } finally {
            widget.close()
        }
    }

    /**
     * What macOS really sends for 안녕하세요 and Enter (AWTView insertText / setMarkedText via CInputMethod): the
     * syllable being composed arrives with nothing committed, a finished syllable arrives alone, fully committed, and
     * the Enter that finishes the last syllable comes after its commit as an ordinary key.
     */
    @Test
    fun theRealMacOsSequenceForAWholeKoreanSentenceSendsEachSyllableOnceAndThenEnter() {
        val connector = RecordingTtyConnector()
        val widget = imeWidget().apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
        }
        widget.start()
        try {
            val panel = widget.terminalPanel
            val steps = listOf(
                listOf("ㅇ", "아", "안") to "안",
                listOf("ㄴ", "녀", "녕") to "녕",
                listOf("ㅎ", "하") to "하",
                listOf("ㅅ", "세") to "세",
                listOf("ㅇ", "요") to "요",
            )
            SwingUtilities.invokeAndWait {
                steps.forEach { (composing, committed) ->
                    composing.forEach { text -> panel.dispatchEvent(inputMethodEvent(panel, text, 0)) }
                    panel.dispatchEvent(inputMethodEvent(panel, committed, committed.length))
                }
                panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_ENTER))
            }
            assertTrue(connector.awaitWrite("안녕하세요\r"))
            assertEquals("안녕하세요\r", connector.writtenText())
        } finally {
            widget.close()
        }
    }

    /** Backspace that empties the composition is the input method's; the next Backspace deletes the character before. */
    @Test
    fun afterTheCompositionIsBackspacedAwayTheNextBackspaceReachesTheTerminal() {
        val connector = RecordingTtyConnector()
        val widget = imeWidget().apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
        }
        widget.start()
        try {
            val panel = widget.terminalPanel
            SwingUtilities.invokeAndWait {
                panel.dispatchEvent(inputMethodEvent(panel, "안", 1))
                panel.dispatchEvent(inputMethodEvent(panel, "ㄴ", 0))
                // macOS clears the marked text with an empty update and sends no key for that Backspace.
                panel.dispatchEvent(inputMethodEvent(panel, "", 0))
                panel.processKeyEvent(keyPressed(panel, KeyEvent.VK_BACK_SPACE))
            }
            assertTrue(connector.awaitWrite("안\u007f"), "wrote ${connector.writtenText().map { it.code }}")
        } finally {
            widget.close()
        }
    }

    private fun keyPressed(panel: TerminalPanel, keyCode: Int): KeyEvent {
        return KeyEvent(
            panel,
            KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            0,
            keyCode,
            KeyEvent.CHAR_UNDEFINED,
        )
    }
}

private class RecordingTtyConnector : TtyConnector {
    private val writes = mutableListOf<String>()
    private val closed = CountDownLatch(1)

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        closed.await()
        return -1
    }

    override fun write(bytes: ByteArray) {
        synchronized(writes) {
            writes += bytes.toString(StandardCharsets.UTF_8)
        }
    }

    override fun write(string: String) {
        synchronized(writes) {
            writes += string
        }
    }

    override fun isConnected(): Boolean {
        return true
    }

    override fun waitFor(): Int {
        return 0
    }

    override fun ready(): Boolean {
        return false
    }

    override fun getName(): String {
        return "recording-tty"
    }

    override fun close() { closed.countDown() }

    override fun resize(termSize: TermSize) = Unit

    override fun init(questioner: Questioner): Boolean {
        return true
    }

    fun awaitWrite(expected: String): Boolean {
        return awaitUntil {
            synchronized(writes) {
                writes.joinToString("").contains(expected)
            }
        }
    }

    fun writtenText(): String = synchronized(writes) { writes.joinToString("") }

    fun awaitWriteCount(expected: Int): Boolean {
        return awaitUntil {
            synchronized(writes) {
                writes.size == expected
            }
        }
    }

    private fun awaitUntil(predicate: () -> Boolean): Boolean {
        repeat(20) {
            if (predicate()) return true
            Thread.sleep(25)
        }
        return predicate()
    }
}
