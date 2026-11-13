package app.thdev.moru.core.terminal

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopJediTermConnectorThemeTest {
    @Test
    fun aProgramThatAskedForThemeNotificationsReceivesTheChange() {
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            // Asks for theme notifications, then shows what arrived on its input in octal so nothing is swallowed.
            startupCommand = "stty raw -echo; printf 'ready\\033[?2031h'; exec dd bs=1 count=9 2>/dev/null | od -An -c",
        ) as ThemeReportingTtyConnector
        val output = StringBuffer()
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) {
                val read = connector.read(buffer, 0, buffer.size)
                if (read < 0) break
                output.append(buffer, 0, read)
            }
        }
        waitFor { output.contains("\u001B[?2031h") }
        connector.reportThemeChange(dark = false)
        waitFor { output.contains("9   ;   2   n") }
        connector.close()
        reader.join(2_000)

        assertTrue(output.contains("033   [   ?   9   9   7   ;   2   n"), "the program read: $output")
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
    }
}
