package app.thdev.moru.core.terminal.host

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import app.thdev.moru.core.terminal.TerminalBackgroundQueryTracker
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * A terminal that lives in the terminal host. Reading gives the host's output (first the replay of what it last
 * showed when Moru reattaches); writing and resizing go to the host. [close] only lets go of it, so the program keeps
 * running for the next time Moru opens; [terminate] ends it. A connection lost while the program still runs in the host
 * (a socket error, a frame it could not read) is made again through [reattach] rather than taken for the program
 * ending, so the terminal does not show as ended while its agent keeps working.
 */
class HostTtyConnector internal constructor(
    channel: SocketChannel,
    private val charset: Charset,
    private val displayName: String,
    /** True when this terminal was started now; false when Moru came back to one that kept running. */
    val created: Boolean,
    val pid: Long,
    private val onExit: (Int?) -> Unit,
    /** Optional Opened metadata; old hosts always replay when reattaching to a nonempty terminal. */
    private val hasReplay: Boolean = !created,
    /** Opens the same terminal on a new connection at the given size; null when the host no longer holds it. */
    private val reattach: ((TermSize?) -> Reattached?)? = null,
) : TtyConnector {
    internal class Reattached(val channel: SocketChannel, val created: Boolean, val hasReplay: Boolean)

    private class Link(val channel: SocketChannel) {
        val input = DataInputStream(SocketStreams.input(channel).buffered(64 * 1024))
        val output = DataOutputStream(SocketStreams.output(channel).buffered())
    }

    @Volatile private var link = Link(channel)
    private val channel get() = link.channel
    private val input get() = link.input
    private val output get() = link.output
    @Volatile private var lastSize: TermSize? = null
    @Volatile private var terminating = false
    private val reader = InputStreamReader(FrameStream(), charset)
    private val ended = CountDownLatch(1)
    @Volatile private var exitCode: Int? = null
    @Volatile private var closed = false
    private val replayBackgroundReplies = AtomicInteger()

    /** Replaying an old query makes the emulator reply again; the retained program did not ask again. */
    internal fun consumeReplayBackgroundReply(): Boolean {
        while (true) {
            val pending = replayBackgroundReplies.get()
            if (pending == 0) return false
            if (replayBackgroundReplies.compareAndSet(pending, pending - 1)) return true
        }
    }

    override fun read(buf: CharArray, offset: Int, length: Int): Int = reader.read(buf, offset, length)

    override fun write(bytes: ByteArray) {
        if (!closed) HostFrames.write(output, HostFrames.Input, bytes)
    }

    override fun write(string: String) = write(string.toByteArray(charset))

    override fun isConnected(): Boolean = !closed && ended.count > 0

    override fun resize(termSize: TermSize) {
        lastSize = termSize
        if (closed) return
        runCatching {
            HostFrames.write(output, HostFrames.Resize, HostFrames.payload { writeInt(termSize.columns); writeInt(termSize.rows) })
        }
    }

    override fun waitFor(): Int {
        ended.await()
        return exitCode ?: -1
    }

    override fun ready(): Boolean = reader.ready()

    override fun getName(): String = displayName

    override fun close() {
        closed = true
        runCatching { channel.close() }
        ended.countDown()
    }

    /** Ends the program in the host, then lets go. */
    fun terminate() {
        // A reconnect in progress sends the Kill on its new connection; see reconnect.
        terminating = true
        runCatching { SocketDeadline.run(channel) { HostFrames.write(output, HostFrames.Kill) } }
        close()
    }

    /** The host's output frames as one byte stream; it ends when the program exits or the host goes away. */
    private inner class FrameStream : InputStream() {
        // Opened(created=false) is followed by the retained terminal's one replay frame, before live output.
        private var firstOutput = true
        private var current = ByteArray(0)
        private var position = 0

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
        }

        override fun read(target: ByteArray, offset: Int, length: Int): Int {
            while (position >= current.size) {
                val read = runCatching { HostFrames.read(input) }
                val frame = read.getOrNull()
                when (frame?.type) {
                    HostFrames.Output -> {
                        // The screen is still showing what a reattach would replay; drawing it again would repeat it.
                        if (skipReplay) {
                            skipReplay = false
                            continue
                        }
                        if (firstOutput && hasReplay) {
                            val queries = TerminalBackgroundQueryTracker()
                            var count = 0
                            frame.payload.forEach { byte ->
                                if (queries.accept((byte.toInt() and 0xff).toChar())) count++
                            }
                            replayBackgroundReplies.addAndGet(count)
                        }
                        firstOutput = false
                        current = frame.payload
                        position = 0
                    }
                    HostFrames.Exit -> {
                        finish(DataInputStream(frame.payload.inputStream()).readInt().takeIf { it >= 0 })
                        return -1
                    }
                    null -> {
                        // Moru let go (close) or the connection was lost; neither means the program ended. Only a
                        // connection that failed is made again: the host ends one cleanly when another connection takes
                        // the terminal over, and taking it back would make the two keep displacing each other.
                        if (!closed && read.isFailure && reconnect()) continue
                        if (!closed) finish(null)
                        return -1
                    }
                    else -> Unit
                }
            }
            val count = minOf(length, current.size - position)
            System.arraycopy(current, position, target, offset, count)
            position += count
            return count
        }

        override fun available(): Int = current.size - position

        private var skipReplay = false

        /** Makes the connection again, a few times, while the host still holds this terminal. */
        private fun reconnect(): Boolean {
            val again = reattach ?: return false
            repeat(ReconnectAttempts) { attempt ->
                Thread.sleep(ReconnectDelayMillis * (attempt + 1))
                if (closed) return false
                val reattached = runCatching { again(lastSize) }.getOrNull() ?: return@repeat
                if (reattached.created) {
                    // The program had ended after all, and the host started a fresh one under the key; end that.
                    runCatching { HostFrames.write(DataOutputStream(SocketStreams.output(reattached.channel)), HostFrames.Kill) }
                    runCatching { reattached.channel.close() }
                    return false
                }
                val previous = link
                link = Link(reattached.channel)
                runCatching { previous.channel.close() }
                if (closed) {
                    // Closed or terminated while reconnecting: the new connection must not be left open, and a Kill sent
                    // on the lost one never arrived.
                    if (terminating) runCatching { SocketDeadline.run(reattached.channel) { HostFrames.write(output, HostFrames.Kill) } }
                    runCatching { reattached.channel.close() }
                    return false
                }
                // What the program printed while the connection was down is in the replay, which is skipped so the screen
                // is not drawn twice; a full-screen program redraws it on the resize below, a plain shell's lines are lost.
                skipReplay = reattached.hasReplay
                // A program redraws on a size change, which repaints whatever it drew while the connection was down.
                lastSize?.let(::resize)
                return true
            }
            return false
        }

        private fun finish(code: Int?) {
            if (ended.count == 0L) return
            exitCode = code
            ended.countDown()
            onExit(code)
        }
    }
}

private const val ReconnectAttempts = 3
private const val ReconnectDelayMillis = 300L
