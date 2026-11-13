package app.thdev.moru.core.terminal

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Desktop lifecycle owner for teardown requested by a UI action. Quit awaits teardown on an IO thread. */
object TerminalTeardown {
    private val executor = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "moru-terminal-teardown").apply { isDaemon = true }
    }

    fun execute(action: () -> Unit) {
        executor.execute { runCatching(action) }
    }

    /** Called off the UI thread during quit, after no more terminal actions can be accepted. */
    fun finish() {
        executor.shutdown()
        executor.awaitTermination(3, TimeUnit.SECONDS)
    }
}
