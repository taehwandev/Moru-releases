package app.thdev.moru.feature.terminal

import com.jediterm.terminal.ui.JediTermWidget
import app.thdev.moru.core.designsystem.theme.darkCkColors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalScreenObservationTest {
    @Test
    fun aBurstReportsTheLatestViewportAndCloseCancelsQueuedUpdates() {
        PreeditTestFixture().use { fixture ->
            val lines = LinkedBlockingQueue<String>()
            lateinit var observer: TerminalScreenObservation
            SwingUtilities.invokeAndWait {
                observer = TerminalScreenObservation(fixture.widget.terminalTextBuffer, lines::offer)
                repeat(500) { fixture.output("\u001B[2J\u001B[Hreading $it") }
            }
            try {
                assertEquals("reading 499", lines.poll(2, TimeUnit.SECONDS))
                assertNull(lines.poll(350, TimeUnit.MILLISECONDS), "Unchanged marked output must not be emitted again.")
                fixture.output("\u001B[2J\u001B[Hqueued after close")
                observer.close()
                assertNull(lines.poll(500, TimeUnit.MILLISECONDS), "Close must cancel an already queued sample.")
            } finally {
                observer.close()
            }
        }
    }

    @Test
    fun aPaletteChangeStillReachesTheNativeTerminal() {
        lateinit var session: JediTermSession
        val holder = TerminalPalette.Holder(TerminalPalette.from(darkCkColors))
        SwingUtilities.invokeAndWait {
            session = JediTermSession(
                shell = "/bin/sh", workingDirectory = System.getProperty("java.io.tmpdir"), startupCommand = null,
                onCommandCaptured = {}, onOutput = {}, onExit = {}, onLocalServerUrl = {}, onOutputLine = {},
                onProcessStarted = {}, fontFamily = null, fontSize = 13, scrollbackLines = 1_000,
                paletteHolder = holder,
            )
        }
        try {
            SwingUtilities.invokeAndWait {
                val widget = session.component() as JediTermWidget
                val before = widget.background
                holder.palette = TerminalPalette.from(app.thdev.moru.core.designsystem.theme.lightCkColors)
                session.applyTheme()
                assertTrue(before != widget.background)
                val changed = widget.background
                repeat(20) { session.applyTheme() }
                assertEquals(changed, widget.background)
            }
        } finally {
            SwingUtilities.invokeAndWait { session.close() }
        }
    }
}
