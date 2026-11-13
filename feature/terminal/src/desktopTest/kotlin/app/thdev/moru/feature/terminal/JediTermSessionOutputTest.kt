package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.lightCkColors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

class JediTermSessionOutputTest {
    @Test
    fun cursorAddressedCodexFrameReportsActivityInsteadOfFooter() {
        val lines = LinkedBlockingQueue<String>()
        lateinit var session: JediTermSession
        SwingUtilities.invokeAndWait {
            session = JediTermSession(
                shell = "/bin/sh",
                workingDirectory = System.getProperty("java.io.tmpdir"),
                startupCommand = "printf '\\033[2J\\033[2;1HWorking on the change\\033[10;1H› \\033[12;1H? for shortcuts  100% context left'; IFS= read -r line; printf '\\033[10;1H› %s' \"\$line\"; sleep 3",
                onCommandCaptured = {}, onOutput = {}, onExit = {}, onLocalServerUrl = {},
                onOutputLine = lines::offer, onProcessStarted = {}, fontFamily = null,
                fontSize = 13, scrollbackLines = 1000,
                paletteHolder = TerminalPalette.Holder(TerminalPalette.from(lightCkColors)),
            )
        }
        try {
            assertTrue(awaitLine(lines, 3) { it == "Working on the change" }, "Sessions should receive activity without the footer.")
            assertTrue(session.type("한글 입력\n"))
            assertTrue(awaitLine(lines, 3) { it == "› 한글 입력" }, "Korean input should survive the PTY and cursor-addressed redraw.")
        } finally {
            SwingUtilities.invokeAndWait { session.close() }
        }
    }

    private fun awaitLine(lines: LinkedBlockingQueue<String>, seconds: Long, matches: (String) -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds)
        while (System.nanoTime() < deadline) {
            val line = lines.poll(100, TimeUnit.MILLISECONDS) ?: continue
            if (matches(line)) return true
        }
        return false
    }
}
