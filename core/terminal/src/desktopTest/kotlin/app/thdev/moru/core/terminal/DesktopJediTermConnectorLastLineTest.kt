package app.thdev.moru.core.terminal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopJediTermConnectorLastLineTest {
    @Test
    fun aRealShellReportsTheLineItPrintedLast() {
        val last = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            // exec replaces the shell, so it prints neither a prompt nor "logout" after the program's own last line.
            startupCommand = "exec printf 'first step\\nsecond step\\n'",
            onOutputLine = { line ->
                last.set(line)
                if (line == "second step") latch.countDown()
            },
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected && latch.count > 0) {
                if (connector.read(buffer, 0, buffer.size) < 0) break
            }
        }
        val reported = latch.await(10, TimeUnit.SECONDS)
        connector.close()
        reader.join(2_000)

        assertTrue(reported, "the last line was not reported: ${last.get()}")
        assertEquals("second step", last.get())
    }
}
