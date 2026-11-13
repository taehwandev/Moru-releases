package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap

/**
 * The agent CLI each terminal is running right now, so a session that started as Claude shows Codex once the user
 * quits Claude and runs `codex` in it. The answer is a cached reading refreshed by [refresh], so asking costs nothing.
 */
object SessionRunningAgents {
    private val agents = ConcurrentHashMap<String, String>()

    /** `claude`, `codex` or `agy` while that agent runs under the session's shell, or null. */
    fun agentFor(sessionId: String): String? = agents[sessionId]

    fun forget(sessionId: String) {
        agents.remove(sessionId)
    }

    /** A fresh reading of the outermost agent only. Its arguments stay local and are never a display label. */
    fun readProcess(sessionId: String): RunningAgentProcess? = SessionShellProcesses.snapshot()[sessionId]
        ?.let { ProcessHandle.of(it).orElse(null) }?.takeIf { it.isAlive }?.let(::outermostAgentUnder)

    /** Re-reads every recorded shell's process tree. Call it off the UI thread. */
    fun refresh() {
        val shells = SessionShellProcesses.snapshot()
        agents.keys.retainAll(shells.keys)
        shells.forEach { (sessionId, shellPid) ->
            val agent = ProcessHandle.of(shellPid).orElse(null)?.takeIf { it.isAlive }?.let(::outermostAgentUnder)
            if (agent == null) agents.remove(sessionId) else agents[sessionId] = agent.program
        }
    }

    /**
     * The agent nearest the shell wins: an agent that runs another agent as a tool is still the one the user is
     * talking to. The shell itself counts, since a startup command can exec into the agent. Processes are read
     * nearest first and the reading stops at the first agent, since reading a command line is the costly part.
     */
    private fun outermostAgentUnder(shell: ProcessHandle): RunningAgentProcess? {
        val tree = listOf(shell) + shell.descendants().use { stream -> stream.toList() }
        if (tree.size == 1) return agentOf(shell)
        val parentOf = tree.associate { process -> process.pid() to process.parent().map { it.pid() }.orElse(null) }
        fun depth(pid: Long): Int = generateSequence(pid) { parentOf[it] }.takeWhile { it != shell.pid() }.count()
        return tree.sortedBy { process -> depth(process.pid()) }.firstNotNullOfOrNull(::agentOf)
    }

    private fun agentOf(process: ProcessHandle): RunningAgentProcess? {
        val info = process.info()
        val command = info.command().orElse(null) ?: return null
        val arguments = info.arguments().map { it.toList() }.orElse(emptyList())
        val program = runningAgentProgram(command, arguments) ?: return null
        return RunningAgentProcess(process.pid(), program, arguments)
    }
}
