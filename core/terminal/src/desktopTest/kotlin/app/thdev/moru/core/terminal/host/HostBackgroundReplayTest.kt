package app.thdev.moru.core.terminal.host

import app.thdev.moru.core.terminal.HostedTtyConnector
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HostBackgroundReplayTest {
    @Test
    fun retainedQueriesDoNotCountAsNewReadsButTheNextLiveQueryDoes() = checkReplies(hasReplay = true)

    @Test
    fun anEmptyReplayDoesNotSwallowTheFirstLiveColorRead() = checkReplies(hasReplay = false)

    @Test
    fun anOlderHostWithoutReplayMetadataStillSuppressesRetainedQueries() = checkReplies(hasReplay = null)

    private fun checkReplies(hasReplay: Boolean?) {
        val folder = Files.createTempDirectory("moru-color-replay").toFile()
        val socket = folder.resolve("host.sock").toPath()
        val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        val peer = AtomicReference<SocketChannel?>()
        val error = AtomicReference<Throwable?>()
        val query = "\u001b]11;?\u0007"
        val reply = "\u001b]11;rgb:1212/3434/5656\u001b\\"
        server.bind(UnixDomainSocketAddress.of(socket))
        val served = thread(isDaemon = true) {
            runCatching {
                server.accept().use { channel ->
                    peer.set(channel)
                    val input = DataInputStream(SocketStreams.input(channel))
                    val output = DataOutputStream(SocketStreams.output(channel))
                    assertEquals(HostFrames.Open, HostFrames.read(input)?.type)
                    HostFrames.write(output, HostFrames.Opened, HostFrames.payload {
                        writeBoolean(false)
                        writeLong(0)
                        if (hasReplay != null) writeBoolean(hasReplay)
                    })
                    HostFrames.write(output, HostFrames.Output, "$query${query}first".toByteArray(UTF_8))
                    repeat(2) { assertInput(input, reply) }
                    HostFrames.write(output, HostFrames.Output, "${query}live".toByteArray(UTF_8))
                    assertInput(input, reply)
                    HostFrames.write(output, HostFrames.Exit, HostFrames.payload { writeInt(0) })
                }
            }.exceptionOrNull()?.let(error::set)
        }
        var connector: HostedTtyConnector? = null
        try {
            TerminalHostLauncher.configureSocketForTest(socket)
            val host = assertNotNull(TerminalHostLauncher.open("colors", listOf("/bin/sh"), folder.path, null, 80, 24, {}))
            val observed = mutableListOf<Int>()
            val wrapped = HostedTtyConnector(host, UTF_8, {}, {}, {}, null, onBackgroundColorRead = { observed += it })
            connector = wrapped
            readUntil(wrapped, "first")
            repeat(2) { wrapped.write(reply) }
            val initialReads = if (hasReplay == false) 2 else 0
            assertEquals(List(initialReads) { 0x123456 }, observed)
            readUntil(wrapped, "live")
            wrapped.write(reply)
            assertEquals(List(initialReads + 1) { 0x123456 }, observed)
            assertEquals(-1, wrapped.read(CharArray(1), 0, 1))
            served.join(5_000)
            assertFalse(served.isAlive)
            assertEquals(null, error.get(), "terminal bytes must pass through unchanged")
        } finally {
            connector?.close()
            peer.get()?.close()
            server.close()
            served.join(1_000)
            folder.deleteRecursively()
        }
    }

    private fun assertInput(input: DataInputStream, expected: String) {
        val frame = assertNotNull(HostFrames.read(input))
        assertEquals(HostFrames.Input, frame.type)
        assertEquals(expected, frame.payload.toString(UTF_8))
    }

    private fun readUntil(connector: HostedTtyConnector, marker: String) {
        val output = StringBuilder()
        val reading = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (!output.contains(marker)) {
                val count = connector.read(buffer, 0, buffer.size)
                if (count < 0) break
                output.append(buffer, 0, count)
            }
        }
        reading.join(5_000)
        assertFalse(reading.isAlive, "host output did not arrive")
        assertTrue(output.contains(marker))
    }
}
