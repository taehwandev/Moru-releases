package app.thdev.moru.core.terminal

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectingTtyConnectorTest {
    private class Recording : TerminableTtyConnector {
        val calls = mutableListOf<String>()
        override fun read(buf: CharArray, offset: Int, length: Int): Int { buf[offset] = 'x'; return 1 }
        override fun write(bytes: ByteArray) { synchronized(calls) { calls += "write:" + String(bytes) } }
        override fun write(string: String) = write(string.toByteArray())
        override fun resize(termSize: TermSize) { synchronized(calls) { calls += "size:${termSize.columns}x${termSize.rows}" } }
        override fun isConnected() = true
        override fun waitFor() = 0
        override fun ready() = true
        override fun getName() = "recording"
        override fun close() { synchronized(calls) { calls += "close" } }
        override fun terminate() { synchronized(calls) { calls += "terminate" } }
    }

    @Test
    fun `a slow connection never holds up its caller, and what was typed meanwhile goes first and in order`() {
        val release = CountDownLatch(1)
        val target = Recording()
        val started = System.nanoTime()
        val connector = connectingTtyConnector({ release.await(); target }, onFailed = {})
        connector.resize(TermSize(80, 24))
        connector.write("ls")
        connector.write("\n")
        assertTrue(System.nanoTime() - started < 1_000_000_000L, "the caller waited for the connection")
        assertTrue(connector.isConnected)

        release.countDown()
        val buf = CharArray(1)
        assertEquals(1, connector.read(buf, 0, 1))
        connector.write("pwd")

        assertEquals(listOf("size:80x24", "write:ls", "write:\n", "write:pwd"), synchronized(target.calls) { target.calls.toList() })
    }

    @Test
    fun `a terminal closed while it connects is ended once it connects, and a failed one reports why`() {
        val release = CountDownLatch(1)
        val target = Recording()
        val connector = connectingTtyConnector({ release.await(); target }, onFailed = {}) as TerminableTtyConnector
        connector.terminate()
        // Closing the widget closes its connector right after.
        connector.close()
        connector.write("typed after closing")
        release.countDown()
        val deadline = System.currentTimeMillis() + 5_000
        while (synchronized(target.calls) { "terminate" !in target.calls } && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertEquals(listOf("terminate"), synchronized(target.calls) { target.calls.toList() })

        val failure = CountDownLatch(1)
        val failing = connectingTtyConnector({ error("no host") }, onFailed = { failure.countDown() })
        assertTrue(failure.await(5, TimeUnit.SECONDS))
        assertEquals(-1, failing.read(CharArray(1), 0, 1))
        assertTrue(!failing.isConnected)
    }
}
