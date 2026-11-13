@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import com.jediterm.terminal.ArrayTerminalDataStream
import com.jediterm.terminal.emulator.JediEmulator
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.FocusEvent
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.font.TextHitInfo
import java.awt.image.BufferedImage
import java.text.AttributedString
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.SwingUtilities

/** Exercises the real Swing terminal with synthetic AWT events, without changing OS focus or input sources. */
internal class PreeditTestFixture(showWindow: Boolean = false, fontSize: Int = 13, dark: Boolean = false) : AutoCloseable {
    val connector = PreeditRecordingTtyConnector()
    val settings = MoruTerminalSettingsProvider(
        null, fontSize, 1_000, TerminalPalette.Holder(TerminalPalette.from(if (dark) darkCkColors else lightCkColors)),
    )
    val widget = onEdt {
        MoruJediTermWidget(80, 10, settings).apply {
            configureMoruInputMethodSupport()
            setTtyConnector(connector)
            setSize(ImageWidth, ImageHeight)
            terminalPanel.setSize(ImageWidth, ImageHeight)
        }
    }
    val panel = widget.terminalPanel
    private val frame = if (showWindow) onEdt {
        JFrame("terminal preedit geometry test").apply {
            setAutoRequestFocus(false)
            setFocusableWindowState(false)
            contentPane.add(widget)
            preferredSize = Dimension(ImageWidth + 32, ImageHeight + 32)
            pack()
            setLocation(-10_000, -10_000)
            isVisible = true
        }
    } else null

    init {
        widget.start()
        output("\u001B[?25l\u001B[3;8H")
    }

    val cellWidth: Int get() = onEdt { panel.getFontMetrics(settings.terminalFont).charWidth('W') }

    fun output(text: String) = onEdt {
        val emulator = JediEmulator(ArrayTerminalDataStream(text.toCharArray()), widget.terminal)
        while (emulator.hasNext()) emulator.next()
    }

    fun composition(text: String, committedCharacters: Int = 0, caret: Int = 0) = onEdt {
        panel.dispatchEvent(
            InputMethodEvent(
                panel,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                AttributedString(text).iterator,
                committedCharacters,
                TextHitInfo.leading(caret),
                null,
            ),
        )
    }

    fun caretChanged(index: Int) = onEdt {
        panel.dispatchEvent(
            InputMethodEvent(panel, InputMethodEvent.CARET_POSITION_CHANGED, TextHitInfo.leading(index), null),
        )
    }

    fun keyPressed(keyCode: Int) = onEdt {
        panel.processKeyEvent(
            KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L, 0, keyCode, KeyEvent.CHAR_UNDEFINED),
        )
    }

    /** Delivers a focus change straight to the panel; the OS focus owner is not touched. */
    fun focus(id: Int) = onEdt {
        MoruTerminalPanel::class.java.getDeclaredMethod("processFocusEvent", FocusEvent::class.java)
            .apply { isAccessible = true }
            .invoke(panel, FocusEvent(panel, id))
    }

    fun candidateLocation(): Rectangle = onEdt {
        val location = requireNotNull(panel.inputMethodRequests).getTextLocation(TextHitInfo.leading(0))
        Rectangle(location).apply {
            if (panel.isShowing) {
                val origin = panel.locationOnScreen
                translate(-origin.x, -origin.y)
            }
        }
    }

    fun paint(): BufferedImage = onEdt {
        BufferedImage(panel.width, panel.height, BufferedImage.TYPE_INT_RGB).also { image ->
            val graphics = image.createGraphics()
            try {
                panel.paint(graphics)
            } finally {
                graphics.dispose()
            }
        }
    }

    override fun close() {
        widget.close()
        onEdt { frame?.dispose() }
    }

    private companion object {
        const val ImageWidth = 640
        const val ImageHeight = 180
    }
}

private fun <T> onEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    val result = AtomicReference<Result<T>>()
    SwingUtilities.invokeAndWait { result.set(runCatching(block)) }
    return result.get().getOrThrow()
}
