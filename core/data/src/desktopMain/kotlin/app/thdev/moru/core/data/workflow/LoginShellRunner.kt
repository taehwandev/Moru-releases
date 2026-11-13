package app.thdev.moru.core.data.workflow

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Runs commands in the user's login shell, so an app started from the Dock finds `git`, `python3` and `curl` where
 * their terminal does. Nothing can be typed in: input is empty, and a command that waits for it ends at its timeout.
 */
class LoginShellRunner(
    private val shell: String = System.getenv("SHELL")?.takeIf { File(it).canExecute() } ?: "/bin/zsh",
) : ShellRunner {
    override fun run(command: String, timeoutMillis: Long, onLine: (String) -> Unit): Int? {
        val process = runCatching {
            ProcessBuilder(shell, "-l", "-c", command)
                .redirectErrorStream(true)
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .start()
        }.getOrElse { error -> onLine("Could not start $shell: ${error.message}"); return null }
        // Lines after the command ended (from something it left running) are not this command's to report.
        val reporting = java.util.concurrent.atomic.AtomicBoolean(true)
        val reader = thread(isDaemon = true, name = "moru-workflow-output") {
            runCatching { process.inputStream.bufferedReader().forEachLine { if (reporting.get()) onLine(it) } }
        }
        return try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                onLine("Stopped after ${timeoutMillis / 60_000} minutes without finishing.")
                null
            } else {
                reader.join(2_000)
                process.exitValue()
            }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } finally {
            reporting.set(false)
            process.descendants().forEach { it.destroyForcibly() }
            if (process.isAlive) process.destroyForcibly()
        }
    }
}

/** [value] as one word for a POSIX shell. */
internal fun shellQuoted(value: String): String = "'" + value.replace("'", "'\\''") + "'"
