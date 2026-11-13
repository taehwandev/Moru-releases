package app.thdev.moru.core.terminal

/**
 * Reads `lsof -nP -iTCP -sTCP:LISTEN -F pcn` into the ports each process listens on. In that output a line starting
 * with `p` begins a process, its `c` line names the command, and each `n` line is one address it listens on. Only
 * ports reachable as localhost count: bound to every interface or to the loopback address. A server bound to one
 * outside address cannot be opened there. Agent CLIs listen on loopback ports for their own use (Codex does in every
 * session); those are not servers to preview, so their ports are left out.
 */
fun parseListeningPorts(lsofFieldOutput: String): Map<Long, Set<Int>> {
    val ports = linkedMapOf<Long, MutableSet<Int>>()
    var pid: Long? = null
    var agent = false
    for (line in lsofFieldOutput.lineSequence()) {
        when {
            line.startsWith("p") -> {
                pid = line.drop(1).toLongOrNull()
                agent = false
            }
            line.startsWith("c") -> agent = line.drop(1).trim().lowercase() in AgentCommands
            line.startsWith("n") -> {
                val owner = pid ?: continue
                if (agent) continue
                val address = line.drop(1)
                val host = address.substringBeforeLast(':', missingDelimiterValue = "")
                val port = address.substringAfterLast(':', missingDelimiterValue = "").toIntOrNull() ?: continue
                if (isLocalHost(host)) ports.getOrPut(owner) { linkedSetOf() }.add(port)
            }
        }
    }
    return ports
}

/** Command names of agent CLIs, whose own listening ports are internal. */
private val AgentCommands = setOf("codex", "claude", "gemini", "agy", "agy-real", "antigravity")

private fun isLocalHost(host: String): Boolean =
    host == "*" || host == "[::]" || host == "[::1]" || host == "localhost" || host.startsWith("127.") || host == "0.0.0.0"
