package app.thdev.moru.core.terminal

import java.util.concurrent.ConcurrentHashMap
import java.io.File

/**
 * The process id of each terminal session's shell, so the app can tell whether the shell is running a program
 * (an agent, a build) or sitting at its prompt. Kept outside the session snapshot, like the other live readings,
 * because it changes without the session changing.
 */
object SessionShellProcesses {
    private val pids = ConcurrentHashMap<String, Long>()
    private val shellExecutables = ConcurrentHashMap<String, String>()
    private val observations = SessionProgramObservations(::snapshot, pids::get, ::hasRunningProgram)

    /** The last background reading, safe to read during composition; null until observed or after replacement. */
    fun observedRunningProgram(sessionId: String): Boolean? = observations.readingOf(sessionId)

    internal fun refreshProgramObservations() = observations.refresh()

    fun record(sessionId: String, pid: Long, expectedShell: String? = null) {
        pids[sessionId] = pid
        val observed = ProcessHandle.of(pid).orElse(null)?.info()?.command()?.orElse(null)
        val executable = expectedShell?.let(::configuredShellName)
            ?: observed?.takeIf { shellName(it) in KnownShells }?.let(::shellName)
        if (executable != null) shellExecutables[sessionId] = executable else shellExecutables.remove(sessionId)
    }

    /** The shell of every session the app started, by session id. */
    fun snapshot(): Map<String, Long> = pids.toMap()

    /**
     * The session whose shell [pid] runs under (the shell itself or any program it started), found by walking up from
     * [pid] to a recorded shell; null when [pid] has ended or belongs to no session.
     */
    fun sessionOf(pid: Long): String? {
        val shells = pids.entries.associate { (sessionId, shell) -> shell to sessionId }
        var process = ProcessHandle.of(pid).orElse(null)
        while (process != null) {
            shells[process.pid()]?.let { return it }
            process = process.parent().orElse(null)
        }
        return null
    }

    fun forget(sessionId: String) {
        pids.remove(sessionId)
        shellExecutables.remove(sessionId)
    }

    /** exec can replace a shell at the same PID; such a program is never an idle shell. */
    internal fun shellProcess(sessionId: String): ProcessHandle? {
        val expected = shellExecutables[sessionId] ?: return null
        return pids[sessionId]?.let { ProcessHandle.of(it).orElse(null) }?.takeIf {
            it.isAlive && shellName(it.info().command().orElse("")) == expected
        }
    }

    private fun shellName(path: String): String = path.substringAfterLast('/').substringAfterLast('\\').removeSuffix(".exe")

    private fun configuredShellName(path: String): String {
        // macOS /bin/sh execs the shell selected by this system-owned link.
        val executable = if (path == "/bin/sh" && System.getProperty("os.name").contains("Mac", ignoreCase = true))
            File("/var/select/sh").takeIf { it.exists() } ?: File("/bin/bash")
        else File(path)
        return shellName(runCatching { executable.canonicalPath }.getOrDefault(path))
    }
    private val KnownShells = setOf("sh", "bash", "zsh", "fish", "dash", "ksh", "csh", "tcsh", "cmd", "powershell", "pwsh")

    /**
     * True while the shell has a running child program, false when it is at its prompt, and null when that cannot
     * be known (no recorded shell, or the shell itself has ended).
     */
    fun hasRunningProgram(sessionId: String): Boolean? {
        val shell = pids[sessionId]?.let { pid -> ProcessHandle.of(pid).orElse(null) } ?: return null
        if (!shell.isAlive) return null
        return shell.children().findAny().isPresent
    }

    /** Live safety check on a worker thread: unknown and exec-replaced shells are never idle. */
    fun isIdleShell(sessionId: String): Boolean = runCatching {
        val shell = shellProcess(sessionId) ?: return false
        if (shellName(shell.info().command().orElse("")) !in KnownShells) return false
        shell.children().use { children -> !children.findAny().isPresent }
    }.getOrDefault(false)

    /**
     * Asks the program running in the session's shell, and everything it started, to quit (SIGTERM), leaving the shell
     * at its prompt. Returns false when there was nothing to stop or the shell is unknown.
     */
    fun stopRunningProgram(sessionId: String): Boolean {
        val shell = pids[sessionId]?.let { pid -> ProcessHandle.of(pid).orElse(null) }?.takeIf { it.isAlive } ?: return false
        val programs = shell.children().toList()
        programs.forEach { program -> program.descendants().forEach { it.destroy() }; program.destroy() }
        return programs.isNotEmpty()
    }
}
