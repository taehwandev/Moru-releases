package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.workbench.launch.AgentConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The user asked to restart session [sessionId]'s agent on its updated CLI: it resumes its own conversation, or shows
 * the CLI's own picker of this folder's conversations when that is not known. An agent that already quit is simply
 * started again. Brings the terminal into view once the line was typed.
 */
suspend fun restartUpdatedAgent(
    sessions: TerminalSessionManager,
    sessionId: String,
    submitLine: (sessionKey: String, line: String) -> Boolean,
) {
    val spec = sessions.listSessions().firstOrNull { it.id == sessionId }?.spec ?: return
    val command = AgentConversation.resumeCommandFor(spec.startupCommand) ?: return
    val running = withContext(Dispatchers.Default) { SessionShellProcesses.hasRunningProgram(sessionId) }
    val typed = if (running == true) restartAgentInSession(sessionId, command, submitLine) else submitLine(sessionId, command)
    if (typed) sessions.selectSession(sessionId)
}
