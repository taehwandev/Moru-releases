package app.thdev.moru.feature.workbench.result

import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec

/**
 * Opens a terminal of the Task in its folder running [command] and brings it into view, so a build's output is
 * where the user already looks. Returns the new session's id, or null if it could not be created.
 */
internal fun startBuildTerminal(
    sessions: TerminalSessionManager,
    taskKey: String,
    workspacePath: String,
    title: String,
    command: String,
): String? =
    runCatching {
        val session = sessions.createTaskTerminalSession(
            TerminalSessionSpec(title = title, workingDirectory = workspacePath, attachedItemId = taskKey, startupCommand = command),
        )
        sessions.selectSession(session.id)
        session.id
    }.getOrNull()
