package app.thdev.moru.core.data.git

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class GitProcessRunner(private val timeoutMillis: Long = 30_000, private val outputLimit: Int = 4_194_304) {
    fun run(root: Path, arguments: List<String>, batchSsh: Boolean = false): GitProcessResult {
        val command = listOf("git", "--no-optional-locks", "--literal-pathspecs", "-c", "credential.interactive=never",
            "-c", "core.askPass=", "-C", root.toString()) + arguments
        val builder = ProcessBuilder(command)
        val environment = builder.environment()
        listOf("GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE", "GIT_COMMON_DIR", "GIT_OBJECT_DIRECTORY",
            "GIT_ALTERNATE_OBJECT_DIRECTORIES", "GIT_NAMESPACE", "GIT_CEILING_DIRECTORIES").forEach(environment::remove)
        environment["GIT_TERMINAL_PROMPT"] = "0"
        environment["GCM_INTERACTIVE"] = "Never"
        environment["GIT_ASKPASS"] = ""
        environment["SSH_ASKPASS"] = "/usr/bin/false"
        environment["SSH_ASKPASS_REQUIRE"] = "never"
        environment["LC_ALL"] = "C"
        if (batchSsh && environment["GIT_SSH_COMMAND"] == null && environment["GIT_SSH"] == null) {
            environment["GIT_SSH_COMMAND"] = "ssh -oBatchMode=yes"
        }
        val process = builder.start()
        process.outputStream.close()
        val clipped = AtomicBoolean(false)
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val readers = listOf(drain(process.inputStream, stdout, clipped), drain(process.errorStream, stderr, clipped))
        var interrupted = false
        val finished = try { process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) {
            interrupted = true
            false
        }
        if (!finished) {
            process.descendants().forEach { it.destroyForcibly() }
            process.destroyForcibly()
        }
        readers.forEach { try { it.join(1_000) } catch (_: InterruptedException) { interrupted = true } }
        val incomplete = readers.any { it.isAlive }
        if (incomplete) {
            process.inputStream.close()
            process.errorStream.close()
        }
        if (interrupted) Thread.currentThread().interrupt()
        val uncertain = !finished || clipped.get() || incomplete || interrupted
        val diagnostic = when {
            !finished -> "Git timed out; its outcome is uncertain. Refresh before trying another action."
            clipped.get() || incomplete -> "Git output exceeded its safe bounds; its outcome is uncertain. Refresh before trying another action."
            else -> GitDiagnosticRedactor.redact(stderr.toString(Charsets.UTF_8))
        }
        return GitProcessResult(if (finished) process.exitValue() else -1, stdout.toByteArray(), diagnostic, uncertain)
    }

    private fun drain(input: InputStream, output: ByteArrayOutputStream, clipped: AtomicBoolean): Thread = Thread {
        try {
            input.use {
                val buffer = ByteArray(8_192)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    synchronized(output) {
                        val room = (outputLimit - output.size()).coerceAtLeast(0)
                        output.write(buffer, 0, count.coerceAtMost(room))
                        if (count > room) clipped.set(true)
                    }
                }
            }
        } catch (_: Exception) { clipped.set(true) }
    }.apply { isDaemon = true; start() }
}
