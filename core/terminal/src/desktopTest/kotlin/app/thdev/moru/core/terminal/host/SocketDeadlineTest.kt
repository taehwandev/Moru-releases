package app.thdev.moru.core.terminal.host

import java.nio.file.Files
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocketDeadlineTest {
    @Test
    fun aPeerThatStopsReadingCannotBlockTheWriterIndefinitely() = withSockets { local, _ ->
        val stopped = CountDownLatch(1)
        thread(isDaemon = true) {
            try {
                SocketDeadline.run(local, 100) { SocketStreams.output(local).write(ByteArray(16 * 1024 * 1024)) }
            } catch (_: Exception) {
                // The timeout closes the socket to interrupt its blocked write.
            } finally { stopped.countDown() }
        }
        assertTrue(stopped.await(2, TimeUnit.SECONDS))
        assertFalse(local.isOpen)
    }

    @Test
    fun detachingUnblocksWaitForWithoutReportingTheProgramAsExited() = withSockets { local, _ ->
        var exited = false
        val connector = HostTtyConnector(local, Charsets.UTF_8, "test", true, 0, { exited = true })
        val stopped = CountDownLatch(1)
        thread(isDaemon = true) { connector.waitFor(); stopped.countDown() }
        connector.close()
        assertTrue(stopped.await(1, TimeUnit.SECONDS))
        assertFalse(exited)
    }

    private fun withSockets(check: (SocketChannel, SocketChannel) -> Unit) {
        val folder = Files.createTempDirectory("moru-socket-deadline")
        try {
            val address = UnixDomainSocketAddress.of(folder.resolve("s"))
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).use { server ->
                server.bind(address)
                SocketChannel.open(StandardProtocolFamily.UNIX).use { local ->
                    local.connect(address)
                    server.accept().use { remote -> check(local, remote) }
                }
            }
        } finally { folder.toFile().deleteRecursively() }
    }
}
