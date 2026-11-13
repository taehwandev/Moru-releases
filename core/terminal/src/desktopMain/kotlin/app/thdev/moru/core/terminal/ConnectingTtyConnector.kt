package app.thdev.moru.core.terminal

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * A terminal that is handed to its view at once while [connect] runs on its own thread: starting or reaching the
 * terminal host can take seconds, and a terminal is opened from the UI thread. Input and a size given meanwhile wait
 * and go, in order, once it is connected; reading waits on the terminal's own reader thread. A connection that fails
 * ends the terminal through [onFailed] instead of failing the view that opened it.
 */
internal class ConnectingTtyConnector(
    connect: () -> TtyConnector,
    private val onFailed: (Throwable) -> Unit,
) : TerminableTtyConnector, ThemeReportingTtyConnector {
    private val lock = Any()
    private val settled = CountDownLatch(1)
    // All guarded by lock.
    private var connected: TtyConnector? = null
    private var failed = false
    private var ending: Ending? = null
    private val pendingInput = mutableListOf<ByteArray>()
    private var pendingSize: TermSize? = null

    private enum class Ending { Close, Terminate }

    init {
        thread(name = "moru-terminal-connect", isDaemon = true) {
            val result = runCatching(connect)
            val (target, end) = synchronized(lock) {
                result.onSuccess { target ->
                    // Flushed before it is published, so later input cannot overtake what was typed while connecting.
                    pendingSize?.let { runCatching { target.resize(it) } }
                    pendingInput.forEach { bytes -> runCatching { target.write(bytes) } }
                    connected = target
                }.onFailure { failed = true }
                pendingInput.clear()
                result.getOrNull() to ending
            }
            settled.countDown()
            result.exceptionOrNull()?.let(onFailed)
            when (end) {
                Ending.Close -> target?.let { runCatching { it.close() } }
                Ending.Terminate -> target?.let { runCatching { (it as? TerminableTtyConnector)?.terminate() ?: it.close() } }
                null -> Unit
            }
        }
    }

    private fun awaitTarget(): TtyConnector? {
        settled.await()
        return synchronized(lock) { connected }
    }

    override fun read(buf: CharArray, offset: Int, length: Int): Int = awaitTarget()?.read(buf, offset, length) ?: -1

    override fun write(bytes: ByteArray) {
        val target = synchronized(lock) {
            connected ?: run {
                if (!failed && ending == null) pendingInput += bytes.copyOf()
                null
            }
        }
        target?.write(bytes)
    }

    override fun write(string: String) = write(string.toByteArray(Charsets.UTF_8))

    override fun resize(termSize: TermSize) {
        val target = synchronized(lock) { connected ?: run { pendingSize = termSize; null } }
        target?.resize(termSize)
    }

    override fun reportThemeChange(dark: Boolean) {
        (synchronized(lock) { connected } as? ThemeReportingTtyConnector)?.reportThemeChange(dark)
    }

    override fun isConnected(): Boolean = synchronized(lock) { connected?.isConnected ?: (!failed && ending == null) }

    override fun ready(): Boolean = synchronized(lock) { connected }?.ready() ?: false

    override fun waitFor(): Int = awaitTarget()?.waitFor() ?: -1

    override fun getName(): String = synchronized(lock) { connected }?.name ?: "Connecting"

    override fun close() = end(Ending.Close)

    override fun terminate() = end(Ending.Terminate)

    private fun end(how: Ending) {
        val target = synchronized(lock) {
            // Closing the widget closes the connector too, right after a terminate; that must not undo the terminate.
            if (connected == null && ending != Ending.Terminate) ending = how
            connected
        }
        when {
            target == null -> Unit
            how == Ending.Terminate && target is TerminableTtyConnector -> target.terminate()
            else -> target.close()
        }
    }
}

/** See [ConnectingTtyConnector]. */
fun connectingTtyConnector(connect: () -> TtyConnector, onFailed: (Throwable) -> Unit): TtyConnector =
    ConnectingTtyConnector(connect, onFailed)
