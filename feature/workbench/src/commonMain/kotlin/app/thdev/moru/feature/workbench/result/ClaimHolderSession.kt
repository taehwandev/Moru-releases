package app.thdev.moru.feature.workbench.result

import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/**
 * The terminal a Moru Core claim or edit [holder] names: a terminal host key (its `MORU_SESSION_ID`), or "pid:N" for
 * an agent whose shell did not pass that on, found by walking up from N to a terminal's shell. Null when none matches.
 */
internal fun claimHolderSession(sessions: TerminalSessionManager, holder: String): TerminalSessionSnapshot? {
    val all = sessions.listSessions()
    val pid = holder.takeIf { it.startsWith("pid:") }?.removePrefix("pid:")?.toLongOrNull()
    if (pid != null) return SessionShellProcesses.sessionOf(pid)?.let { id -> all.firstOrNull { it.id == id } }
    return all.firstOrNull { it.spec.hostKey == holder }
}

/** How a Live Web mark names the agent in [session]: the program it started ("Claude"), else the terminal's title. */
internal fun agentNameOf(session: TerminalSessionSnapshot): String =
    session.spec.startupCommand?.trim()?.substringBefore(' ')?.substringAfterLast('/')?.takeIf(String::isNotBlank)
        ?.replaceFirstChar { it.uppercase() }
        ?: session.spec.title
        ?: session.id
