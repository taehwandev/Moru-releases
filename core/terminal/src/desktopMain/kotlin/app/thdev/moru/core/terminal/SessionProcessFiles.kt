package app.thdev.moru.core.terminal

import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * A terminal's shell and every program under it: their [pids], and the folders they work in and files they hold right
 * now. An agent keeps its own process in the folder it started in and runs its commands in the folder it works in, so
 * the commands, not the agent, tell which worktree it is changing while they run. The files an agent holds open name
 * its conversation record, which tells the same after the commands end.
 */
data class SessionProcessFiles(
    val pids: List<Long>,
    val files: ProcessFiles,
) {
    companion object {
        /** Reads the session's tree with one `lsof`; null when its shell is gone. Call it off the UI thread. */
        fun read(sessionId: String): SessionProcessFiles? {
            val shell = SessionShellProcesses.snapshot()[sessionId]
                ?.let { pid -> ProcessHandle.of(pid).orElse(null) }
                ?.takeIf { process -> process.isAlive }
                ?: return null
            val pids = listOf(shell.pid()) + shell.descendants().use { stream -> stream.map { it.pid() }.toList() }
            val files = runLsof(pids)?.let(::parseLsofProcessFiles) ?: ProcessFiles(emptyList(), emptyList())
            return SessionProcessFiles(pids, files)
        }

        /** The selected provider's own files, without borrowing records held by a nested agent. */
        fun readProcess(pid: Long): SessionProcessFiles? {
            val process = ProcessHandle.of(pid).orElse(null)?.takeIf { it.isAlive } ?: return null
            val pids = listOf(process.pid())
            val files = runLsof(pids)?.let(::parseLsofProcessFiles) ?: ProcessFiles(emptyList(), emptyList())
            return SessionProcessFiles(pids, files)
        }

        // lsof exits 1 when one of the processes ended before it looked, and still prints the others.
        private fun runLsof(pids: List<Long>): String? =
            runCatching {
                val process = ProcessBuilder("lsof", "-p", pids.joinToString(","), "-F", "pfn")
                    .redirectErrorStream(false)
                    .start()
                var text = ""
                val reader = thread(isDaemon = true, name = "moru-lsof-files") { text = process.inputStream.bufferedReader().readText() }
                if (!process.waitFor(LsofTimeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return null
                }
                reader.join(2_000)
                text
            }.getOrNull()

        private const val LsofTimeoutSeconds = 2L
    }
}
