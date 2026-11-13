package app.thdev.moru.core.terminal

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/** Owns a transient command, its bounded stdout, and one deadline for output and process exit. */
object BoundedProcessRunner {
    const val TextLimitBytes = 1024 * 1024
    const val PngLimitBytes = 16 * 1024 * 1024

    @Throws(InterruptedException::class)
    fun run(
        command: List<String>,
        timeoutMillis: Long,
        maxOutputBytes: Int = TextLimitBytes,
        workingDirectory: File? = null,
        mergeErrors: Boolean = false,
    ): BoundedProcessOutput? {
        require(timeoutMillis > 0 && maxOutputBytes > 0)
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Command interrupted.")
        val startedAt = System.nanoTime()
        val timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        val process = try {
            ProcessBuilder(command).apply {
                directory(workingDirectory)
                if (mergeErrors) redirectErrorStream(true) else redirectError(ProcessBuilder.Redirect.DISCARD)
            }.start()
        } catch (error: Exception) {
            rethrowIfInterrupted(error)
            return null
        }
        return try {
            process.outputStream.close()
            collect(process, startedAt, timeoutNanos, maxOutputBytes)
        } catch (interrupted: InterruptedException) {
            throw interrupted
        } catch (error: Exception) {
            rethrowIfInterrupted(error)
            null
        } finally {
            closeOwnedProcess(process)
        }
    }

    private fun collect(process: Process, startedAt: Long, timeoutNanos: Long, limit: Int): BoundedProcessOutput? {
        val output = ByteArrayOutputStream(minOf(limit, 8192))
        val buffer = ByteArray(8192)
        var completed = false
        while (true) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Command interrupted.")
            // Read only bytes that the pipe says are ready. No blocking reader task outlives this call.
            var available = process.inputStream.available()
            while (available > 0) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("Command interrupted.")
                if (System.nanoTime() - startedAt >= timeoutNanos) return null
                val count = process.inputStream.read(buffer, 0, minOf(buffer.size, available))
                if (count < 0) break
                if (count > limit - output.size()) return null
                output.write(buffer, 0, count)
                available = process.inputStream.available()
            }
            val remaining = timeoutNanos - (System.nanoTime() - startedAt)
            if (remaining <= 0) return null
            if (completed) return BoundedProcessOutput(process.exitValue(), output.toByteArray())
            completed = process.waitFor(minOf(remaining, PollIntervalNanos), TimeUnit.NANOSECONDS)
        }
    }

    private fun rethrowIfInterrupted(error: Exception) {
        if (error is InterruptedException) throw error
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedException("Command interrupted.").apply { initCause(error) }
        }
    }

    private fun closeOwnedProcess(process: Process) {
        var restoreInterrupt = Thread.interrupted()
        if (process.isAlive) {
            val descendants = runCatching { process.descendants().use { it.toList() } }.getOrDefault(emptyList())
            descendants.asReversed().forEach { runCatching { it.destroyForcibly() } }
            try {
                // Give a waiting parent a chance to reap its terminated children before terminating it.
                if (descendants.isNotEmpty()) process.waitFor(50, TimeUnit.MILLISECONDS)
                if (process.isAlive) process.destroyForcibly()
                process.waitFor(150, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                restoreInterrupt = true
                runCatching { process.destroyForcibly() }
            }
        }
        runCatching { process.outputStream.close() }
        runCatching { process.inputStream.close() }
        runCatching { process.errorStream.close() }
        if (restoreInterrupt) Thread.currentThread().interrupt()
    }

    private val PollIntervalNanos = TimeUnit.MILLISECONDS.toNanos(5)
}
