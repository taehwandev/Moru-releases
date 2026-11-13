package app.thdev.moru.core.terminal.host

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** A lost connection to a terminal the host still runs is made again, instead of the terminal showing as ended. */
class HostReconnectTest {
    @Test
    fun aDroppedConnectionReattachesAndTheOutputGoesOn() {
        val folder = Files.createTempDirectory("moru-host-reconnect").toFile()
        val socket = folder.resolve("h.sock").toPath()
        val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(socket))
        val opens = AtomicInteger()
        thread(isDaemon = true) {
            while (server.isOpen) {
                val channel = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { serve(channel, opens) }
            }
        }
        val exits = CopyOnWriteArrayList<Int?>()
        try {
            TerminalHostLauncher.configureSocketForTest(socket)
            val connector = assertNotNull(TerminalHostLauncher.open("agent", listOf("/bin/sh"), folder.path, "", 80, 24, { exits += it }))

            val seen = StringBuilder()
            val buffer = CharArray(256)
            val deadline = System.currentTimeMillis() + 10_000
            while (!seen.contains("after") && System.currentTimeMillis() < deadline) {
                val count = connector.read(buffer, 0, buffer.size)
                if (count < 0) break
                seen.append(buffer, 0, count)
            }

            assertTrue(seen.contains("before") && seen.contains("after"), "output went on after the reconnect: $seen")
            assertTrue(!seen.contains("replayed"), "the screen is not drawn twice: $seen")
            assertEquals(2, opens.get())
            assertTrue(exits.isEmpty(), "the terminal never showed as ended")
            connector.close()
        } finally {
            server.close()
            folder.deleteRecursively()
        }
    }

    @Test
    fun aConnectionTheHostEndsCleanlyIsNotTakenBack() {
        val folder = Files.createTempDirectory("moru-host-displaced").toFile()
        val socket = folder.resolve("h.sock").toPath()
        val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(socket))
        val opens = AtomicInteger()
        thread(isDaemon = true) {
            while (server.isOpen) {
                val channel = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    val input = DataInputStream(SocketStreams.input(channel))
                    val output = DataOutputStream(SocketStreams.output(channel).buffered())
                    while (true) {
                        val frame = runCatching { HostFrames.read(input) }.getOrNull() ?: return@thread
                        if (frame.type == HostFrames.Held) HostFrames.write(output, HostFrames.HeldKeys, HostFrames.payload { writeInt(1); writeUTF("agent") })
                        if (frame.type != HostFrames.Open) continue
                        opens.incrementAndGet()
                        HostFrames.write(output, HostFrames.Opened, HostFrames.payload { writeBoolean(true); writeLong(7L); writeBoolean(false) })
                        // Another connection took the terminal over; the host ends this one cleanly.
                        channel.close()
                        return@thread
                    }
                }
            }
        }
        val exits = CopyOnWriteArrayList<Int?>()
        try {
            TerminalHostLauncher.configureSocketForTest(socket)
            val connector = assertNotNull(TerminalHostLauncher.open("agent", listOf("/bin/sh"), folder.path, "", 80, 24, { exits += it }))

            assertEquals(-1, connector.read(CharArray(16), 0, 16))
            Thread.sleep(1_000)
            assertEquals(1, opens.get(), "a displaced connection does not open the terminal again")
            assertEquals(listOf<Int?>(null), exits.toList())
        } finally {
            server.close()
            folder.deleteRecursively()
        }
    }

    private fun serve(channel: SocketChannel, opens: AtomicInteger) {
        val input = DataInputStream(SocketStreams.input(channel))
        val output = DataOutputStream(SocketStreams.output(channel).buffered())
        while (true) {
            val frame = runCatching { HostFrames.read(input) }.getOrNull() ?: return
            when (frame.type) {
                HostFrames.Held -> HostFrames.write(output, HostFrames.HeldKeys, HostFrames.payload { writeInt(1); writeUTF("agent") })
                HostFrames.Open -> {
                    if (opens.incrementAndGet() == 1) {
                        HostFrames.write(output, HostFrames.Opened, HostFrames.payload { writeBoolean(true); writeLong(7L); writeBoolean(false) })
                        HostFrames.write(output, HostFrames.Output, "before ".toByteArray())
                        // The connection breaks (here, a frame that cannot be read) while the program keeps running.
                        output.writeByte(HostFrames.Output.toInt())
                        output.writeInt(HostFrames.MaxPayload + 1)
                        output.flush()
                        return
                    }
                    HostFrames.write(output, HostFrames.Opened, HostFrames.payload { writeBoolean(false); writeLong(7L); writeBoolean(true) })
                    HostFrames.write(output, HostFrames.Output, "replayed ".toByteArray())
                    HostFrames.write(output, HostFrames.Output, "after".toByteArray())
                }
                else -> Unit
            }
        }
    }
}
