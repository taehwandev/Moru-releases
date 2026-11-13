package app.thdev.moru.app

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime

/**
 * Keeps what Moru reports on stderr in `logs/moru.log` under the app data folder, and records every uncaught exception
 * there with its thread, so a crash or a failure in the packaged app (which has no console) leaves a trace. The log
 * starts over once it grows past [MaxBytes], keeping the previous one as `moru.log.1`.
 */
internal object MoruErrorLog {
    private const val MaxBytes = 4L * 1024 * 1024

    /** Where the log of the app using [appDataDir] is. */
    fun path(appDataDir: File): File = appDataDir.resolve("logs").resolve("moru.log")

    fun install(appDataDir: File, onUncaught: (Thread, Throwable) -> Unit = { _, _ -> }) {
        val log = runCatching { open(appDataDir.resolve("logs")) }.getOrNull()
        if (log != null) System.setErr(PrintStream(Tee(System.err, log), true, Charsets.UTF_8))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                System.err.println("${LocalDateTime.now()} Uncaught in ${thread.name}:")
                error.printStackTrace()
            }
            runCatching { onUncaught(thread, error) }
            previous?.uncaughtException(thread, error)
        }
        System.err.println("${LocalDateTime.now()} Moru started")
    }

    private fun open(dir: File): OutputStream {
        dir.mkdirs()
        val file = dir.resolve("moru.log")
        if (file.length() > MaxBytes) file.renameTo(dir.resolve("moru.log.1").apply { delete() })
        return FileOutputStream(file, true)
    }

    /** Writes to the console as before, and to the log; a log that can no longer be written is dropped. */
    private class Tee(private val console: OutputStream, private val log: OutputStream) : OutputStream() {
        @Volatile private var logOpen = true

        override fun write(b: Int) {
            console.write(b)
            toLog { log.write(b) }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            console.write(b, off, len)
            toLog { log.write(b, off, len) }
        }

        override fun flush() {
            console.flush()
            toLog { log.flush() }
        }

        private inline fun toLog(action: () -> Unit) {
            if (logOpen) runCatching(action).onFailure { logOpen = false }
        }
    }
}
