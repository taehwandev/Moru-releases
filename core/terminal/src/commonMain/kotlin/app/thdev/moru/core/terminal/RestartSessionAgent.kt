package app.thdev.moru.core.terminal

/** Stops only the named agent, then submits its resume line to the same known-idle shell. */
internal fun restartSessionAgent(
    sessionId: String,
    executable: String,
    command: String,
    canRestart: () -> Boolean,
    submitLine: (String, String) -> Boolean,
): Boolean {
    val shell = SessionShellProcesses.shellProcess(sessionId) ?: return false
    if (!canRestart()) return false
    val programs = shell.children().use { it.toList() }
    if (programs.isNotEmpty()) {
        val agent = programs.singleOrNull() ?: return false
        val started = agent.info().startInstant()
        fun isAgent(): Boolean = agent.isAlive && agent.info().startInstant() == started &&
            SessionShellProcesses.shellProcess(sessionId)?.pid() == shell.pid() &&
            agent.info().command().orElse("").substringAfterLast('/').substringAfterLast('\\') == executable &&
            shell.children().use { it.toList().map(ProcessHandle::pid) } == listOf(agent.pid())
        if (!isAgent() || !canRestart() || !isAgent()) return false
        agent.descendants().use { descendants -> descendants.forEach { it.destroy() } }
        if (!agent.destroy()) return false
        val deadline = System.nanoTime() + 10_000_000_000L
        while (agent.isAlive && System.nanoTime() < deadline) Thread.sleep(50)
        if (agent.isAlive) return false
    }
    // A replacement server or build must never receive an agent resume command.
    if (!canRestart() || SessionShellProcesses.shellProcess(sessionId)?.pid() != shell.pid()) return false
    if (shell.children().use { it.anyMatch { child -> child.isAlive } }) return false
    return submitLine(sessionId, command)
}
