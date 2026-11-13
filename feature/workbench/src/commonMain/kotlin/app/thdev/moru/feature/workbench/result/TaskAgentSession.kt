package app.thdev.moru.feature.workbench.result

import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/**
 * The terminal of [taskKey] its agent is working in: the one in view if it belongs to the Task, otherwise the
 * first of the Task's sessions, otherwise none.
 */
internal fun taskAgentSession(sessions: TerminalSessionManager, taskKey: String): TerminalSessionSnapshot? {
    val ofTask = sessions.listSessions().filter { it.spec.attachedItemId == taskKey }
    return ofTask.firstOrNull { it.id == sessions.selectedSessionId() } ?: ofTask.firstOrNull()
}
