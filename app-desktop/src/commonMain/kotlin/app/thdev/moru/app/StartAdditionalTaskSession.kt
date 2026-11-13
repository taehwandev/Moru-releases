package app.thdev.moru.app

import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.terminal.releaseTerminalSession
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.agentLaunchCommand

/** Starts another terminal of the living Task without creating or retargeting that Task. */
internal fun startAdditionalTaskSession(
    sessions: TerminalSessionManager,
    taskId: String,
    folder: String,
    shell: String,
    runtime: AgentRuntime,
    startTerminal: (TerminalSessionSnapshot) -> Unit,
): Result<TerminalSessionSnapshot> {
    val previousSelection = sessions.selectedSessionId()
    var created: TerminalSessionSnapshot? = null
    return runCatching {
        sessions.createTaskTerminalSession(
            TerminalSessionSpec(
                title = runtime.displayName,
                shell = shell,
                workingDirectory = folder,
                attachedItemId = taskId,
                startupCommand = agentLaunchCommand(runtime, ""),
            ),
        ).also { session ->
            created = session
            startTerminal(session)
            sessions.selectSession(session.id)
        }
    }.onFailure {
        created?.let { session ->
            runCatching { sessions.closeSession(session.id) }
            runCatching { releaseTerminalSession(session.id) }
        }
        previousSelection?.let { if (sessions.selectedSessionId() != it) sessions.selectSession(it) }
    }
}
