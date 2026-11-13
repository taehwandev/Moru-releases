@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package app.thdev.moru.feature.terminal

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.Questioner
import com.jediterm.terminal.TtyConnector
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Records the actual JediTerm input queue without starting or writing to a user process. */
internal class PreeditRecordingTtyConnector : TtyConnector {
    private val writes = mutableListOf<String>()
    private val firstWrite = CountDownLatch(1)
    private val closed = CountDownLatch(1)

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        closed.await()
        return -1
    }

    override fun write(bytes: ByteArray) = write(bytes.toString(StandardCharsets.UTF_8))

    override fun write(string: String) {
        synchronized(writes) { writes += string }
        firstWrite.countDown()
    }

    override fun isConnected(): Boolean = true
    override fun waitFor(): Int = 0
    override fun ready(): Boolean = false
    override fun getName(): String = "preedit-recording-tty"
    override fun close() = closed.countDown()
    override fun resize(termSize: TermSize) = Unit
    override fun init(questioner: Questioner): Boolean = true

    fun hasWriteWithin(milliseconds: Long): Boolean = firstWrite.await(milliseconds, TimeUnit.MILLISECONDS)
    fun writtenText(): String = synchronized(writes) { writes.joinToString("") }

    fun awaitText(expected: String): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < deadline) {
            if (writtenText() == expected) return true
            Thread.sleep(10)
        }
        return writtenText() == expected
    }
}
