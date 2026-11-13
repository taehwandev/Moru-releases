package app.thdev.moru.core.terminal.host

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/** A host too slow to answer may already run the program; Moru asks it again instead of starting a second copy. */
class HostOpenRetryTest {
    @Test
    fun aHostThatDidNotAnswerInTimeIsAskedAgainForTheSameTerminal() {
        val folder = Files.createTempDirectory("moru-host-retry").toFile()
        val socket = folder.resolve("h.sock").toPath()
        val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(socket))
        val opens = AtomicInteger()
        val keys = mutableListOf<String>()
        thread(isDaemon = true) {
            while (server.isOpen) {
                val channel = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    val input = DataInputStream(SocketStreams.input(channel))
                    val frame = runCatching { HostFrames.read(input) }.getOrNull() ?: return@thread
                    if (frame.type != HostFrames.Open) return@thread
                    synchronized(keys) { keys += DataInputStream(frame.payload.inputStream()).readUTF() }
                    // The first Open starts the program but is answered too late; the second finds it running.
                    if (opens.incrementAndGet() == 1) return@thread
                    HostFrames.write(
                        DataOutputStream(SocketStreams.output(channel).buffered()),
                        HostFrames.Opened,
                        HostFrames.payload { writeBoolean(false); writeLong(42L); writeBoolean(true) },
                    )
                    // Keep the connection open as a real host does.
                    runCatching { input.read() }
                }
            }
        }
        try {
            TerminalHostLauncher.configureSocketForTest(socket)

            val connector = assertNotNull(TerminalHostLauncher.open("agent", listOf("/bin/sh"), folder.path, "claude", 80, 24, {}))

            assertFalse(connector.created, "the second answer attaches to the program the first Open started")
            assertEquals(42L, connector.pid)
            assertEquals(listOf("agent", "agent"), synchronized(keys) { keys.toList() })
            connector.close()
        } finally {
            server.close()
            folder.deleteRecursively()
        }
    }
}
