package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.terminal.createDesktopJediTermTtyConnector
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

/** Proves the native key adaptation reaches a real temporary sh PTY, rather than only a recording connector. */
class TerminalModifiedEnterPtyTest {
    @Test
    fun shiftEnterReachesTheProcessAsOneModifiedKey() {
        val directory = Files.createTempDirectory("moru-modified-enter").toFile()
        val ready = CountDownLatch(1)
        val bytes = CountDownLatch(1)
        val exits = CountDownLatch(1)
        val widget = MoruJediTermWidget(80, 10,
            MoruTerminalSettingsProvider(null, 13, 1000, TerminalPalette.Holder(TerminalPalette.from(lightCkColors))))
        try {
            val connector = createDesktopJediTermTtyConnector(
                shell = "/bin/sh", workingDirectory = directory.absolutePath,
                startupCommand = "stty raw -echo; printf 'ready\\r\\n'; od -An -tx1 -N7; exit",
                onOutputLine = { line ->
                    if (line == "ready") ready.countDown()
                    if (line.trim().replace(Regex("\\s+"), " ") == "1b 5b 31 33 3b 32 75") bytes.countDown()
                },
                onExit = { exits.countDown() },
            )
            widget.setTtyConnector(connector)
            widget.start()
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Temporary PTY did not become ready")
            SwingUtilities.invokeAndWait {
                val panel = widget.terminalPanel
                panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_ENTER, '\n'))
                panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_TYPED, 1, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UNDEFINED, '\n'))
                panel.processKeyEvent(KeyEvent(panel, KeyEvent.KEY_RELEASED, 2, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_ENTER, '\n'))
            }
            assertTrue(bytes.await(10, TimeUnit.SECONDS), "The process must receive exactly the modified Enter bytes")
            assertTrue(exits.await(10, TimeUnit.SECONDS), "Temporary PTY did not exit")
        } finally {
            widget.close()
            directory.deleteRecursively()
        }
    }
}
