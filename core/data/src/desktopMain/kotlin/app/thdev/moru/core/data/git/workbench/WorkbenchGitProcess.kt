package app.thdev.moru.core.data.git.workbench

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Runs one git command and collects what it printed. The one place the app starts git for reading or preparing a repo. */
internal object WorkbenchGitProcess {
    class Run(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val truncated: Boolean,
        val timedOut: Boolean,
    ) {
        fun firstErrorLine(): String? = stderr.lineSequence().firstOrNull { line -> line.isNotBlank() }
    }

    /**
     * Runs git in [directory], reading output while it runs: a large patch overflows the pipe buffer, and
     * waiting for exit first would hang. Output past [maxChars] stops the process and is reported as
     * truncated. Null when git cannot be started at all.
     */
    fun run(directory: File, vararg args: String, maxChars: Int = DefaultMaxChars): Run? {
        val process = try {
            ProcessBuilder(listOf("git", "-C", directory.absolutePath, "-c", "core.quotepath=false") + args)
                .also { builder -> builder.environment()["LC_ALL"] = "C" }
                .start()
        } catch (error: Exception) {
            return null
        }

        val stdout = StringBuilder()
        var stderr = ""
        var truncated = false
        val errReader = thread(isDaemon = true, name = "moru-git-err") {
            stderr = process.errorStream.bufferedReader(Charsets.UTF_8).readText()
        }
        val outReader = thread(isDaemon = true, name = "moru-git-out") {
            val reader = process.inputStream.bufferedReader(Charsets.UTF_8)
            val buffer = CharArray(ReadChunk)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                if (stdout.length + read > maxChars) {
                    stdout.append(buffer, 0, maxChars - stdout.length)
                    truncated = true
                    process.destroyForcibly()
                    break
                }
                stdout.append(buffer, 0, read)
            }
        }

        if (!process.waitFor(TimeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return Run("", "", -1, truncated = false, timedOut = true)
        }
        outReader.join(ReaderJoinMillis)
        errReader.join(ReaderJoinMillis)
        return Run(stdout.toString(), stderr, process.exitValue(), truncated, timedOut = false)
    }

    private const val DefaultMaxChars = 2_000_000
    private const val ReadChunk = 8_192
    private const val TimeoutSeconds = 15L
    private const val ReaderJoinMillis = 2_000L
}
