package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Local servers that a terminal's programs are running, found by the ports the processes under its shell listen on.
 * It catches a server an agent started without printing its address, which the output reader cannot see. The answer is
 * a cached reading refreshed by [refresh], so asking for it costs nothing and never runs a command.
 */
object ListeningServers {
    /** Each session's web server ports, newest process first. */
    private val ports = ConcurrentHashMap<String, List<Int>>()

    /** The address of a server running under the session's terminal, or null if none is. */
    fun urlFor(sessionId: String): String? = ports[sessionId]?.firstOrNull()?.let { port -> "http://localhost:$port/" }

    /** Every port a web server under the session's terminal listens on. */
    fun portsFor(sessionId: String): Set<Int> = ports[sessionId].orEmpty().toSet()

    fun forget(sessionId: String) {
        ports.remove(sessionId)
    }

    /**
     * Reads who is listening now and updates every session. [listening] gives `lsof -F pcn` output, or null when it cannot
     * be had, in which case what was known is left as it was. Call it off the UI thread.
     *
     * A server only runs under a shell that is running a program, so when no terminal is, nothing can be listening:
     * every address is dropped and `lsof`, which costs a process and a scan of every socket, is not run at all.
     */
    fun refresh(listening: () -> String? = ::runLsof, respondsToHttp: (Int) -> Boolean = ::respondsToPreviewHttp) {
        if (SessionShellProcesses.snapshot().keys.none { sessionId -> SessionShellProcesses.hasRunningProgram(sessionId) == true }) {
            ports.clear()
            verdicts.clear()
            return
        }
        val output = listening() ?: return
        val portsByPid = parseListeningPorts(output)
        val startByPid = portsByPid.keys.associateWith(::startOf)
        val listeners = portsByPid.flatMap { (pid, pidPorts) -> pidPorts.map { port -> Listener(pid, port, startByPid[pid]) } }.toSet()
        // A listener that closed, or a port reused by another process, is asked afresh.
        verdicts.keys.retainAll(listeners)
        SessionShellProcesses.snapshot().forEach { (sessionId, shellPid) ->
            val served = ProcessHandle.of(shellPid).orElse(null)
                ?.takeIf { shell -> shell.isAlive }
                ?.descendants()?.use { stream ->
                    // Prefer the newest process, but an internal TCP socket is not a web server.
                    stream.map { process -> process.pid() }.toList()
                }
                ?.sortedDescending()
                ?.flatMap { pid -> portsByPid[pid].orEmpty().sorted().map { port -> Listener(pid, port, startByPid[pid]) } }
                ?.distinctBy { listener -> listener.port }
                ?.filter { listener -> servesPage(listener, respondsToHttp) }
                ?.map { listener -> listener.port }
                .orEmpty()
            if (served.isEmpty()) ports.remove(sessionId) else ports[sessionId] = served
        }
    }

    /**
     * Asking is a real `GET /`, and a dev server renders its page for it (database reads, paid APIs), so each listener is
     * asked once. A page stays a page while that process holds the port. A listener that is not a page yet may be a dev
     * server still starting, so it is asked again on the next scans, [MaxProbes] times in all, then left alone.
     */
    private fun servesPage(listener: Listener, respondsToHttp: (Int) -> Boolean): Boolean {
        val known = verdicts[listener]
        if (known != null && (known.page || known.probes >= MaxProbes)) return known.page
        val page = respondsToHttp(listener.port)
        verdicts[listener] = Verdict(page = page, probes = (known?.probes ?: 0) + 1)
        return page
    }

    private fun startOf(pid: Long): java.time.Instant? =
        ProcessHandle.of(pid).flatMap { process -> process.info().startInstant() }.orElse(null)

    /** One process listening on one port; the start time tells a reused pid apart. */
    private data class Listener(val pid: Long, val port: Int, val started: java.time.Instant?)

    private data class Verdict(val page: Boolean, val probes: Int)

    private val verdicts = ConcurrentHashMap<Listener, Verdict>()

    private const val MaxProbes = 3

    private fun runLsof(): String? =
        runCatching {
            val process = ProcessBuilder("lsof", "-nP", "-iTCP", "-sTCP:LISTEN", "-F", "pcn").redirectErrorStream(false).start()
            var text = ""
            val reader = thread(isDaemon = true, name = "moru-lsof") { text = process.inputStream.bufferedReader().readText() }
            if (!process.waitFor(LsofTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            reader.join(2_000)
            text
        }.getOrNull()

    private const val LsofTimeoutSeconds = 4L
}
