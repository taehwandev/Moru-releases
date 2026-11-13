package app.thdev.moru.app

import app.thdev.moru.core.terminal.TerminalSessionManager

/** Deletes a Task: its terminals are stopped first, then it leaves the catalog. Its working folder is never touched. */
internal fun WorkbenchNavigation.deleteTaskWithSessions(
    taskId: String,
    sessions: TerminalSessionManager,
    terminateHeld: (hostKey: String) -> Unit = {},
) {
    sessions.listSessions().filter { session -> session.spec.attachedItemId == taskId }.forEach { session ->
        endTerminalSession(session, sessions, terminateHeld)
    }
    deleteTask(taskId)
}
