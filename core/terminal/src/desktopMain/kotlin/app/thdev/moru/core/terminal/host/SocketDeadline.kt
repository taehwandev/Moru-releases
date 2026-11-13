package app.thdev.moru.core.terminal.host

import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Closing the channel interrupts a blocked socket write; thread interruption alone cannot safely do that. */
object SocketDeadline {
    private val timer = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "moru-terminal-socket-deadline").apply { isDaemon = true }
    }

    fun <T> run(channel: SocketChannel, timeoutMillis: Long = 500, action: () -> T): T {
        val deadline = timer.schedule({ runCatching { channel.close() } }, timeoutMillis, TimeUnit.MILLISECONDS)
        return try { action() } finally { deadline.cancel(false) }
    }
}
