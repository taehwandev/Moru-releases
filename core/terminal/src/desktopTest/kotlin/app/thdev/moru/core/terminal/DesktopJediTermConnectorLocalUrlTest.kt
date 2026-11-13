package app.thdev.moru.core.terminal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopJediTermConnectorLocalUrlTest {
    @Test
    fun aRealShellPrintingADevServerAddressReportsIt() {
        val found = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            startupCommand = "echo '  Local:   http://localhost:4321/'",
            onLocalServerUrl = { url -> found.set(url); latch.countDown() },
        )
        // The connector only inspects output as it is read, like the terminal widget does.
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected && latch.count > 0) {
                if (connector.read(buffer, 0, buffer.size) < 0) break
            }
        }
        val reported = latch.await(10, TimeUnit.SECONDS)
        connector.close()
        reader.join(2_000)

        assertTrue(reported, "the address was not reported")
        assertEquals("http://localhost:4321/", found.get())
    }
}
