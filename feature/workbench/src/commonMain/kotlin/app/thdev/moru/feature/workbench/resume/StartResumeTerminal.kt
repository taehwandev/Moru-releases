package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.result.startBuildTerminal

/**
 * Opens a new terminal of the Task in its folder that reopens [runtime]'s conversation of this Task, the one in the
 * Task's latest terminal of that agent, and brings it into view. Without a known conversation in that terminal it
 * does nothing. It opens a new terminal and never types into an existing agent. Returns null when the target is
 * unavailable or the terminal could not be created; provider failure stays visible in the new terminal.
 */
internal fun startResumeTerminal(
    sessions: TerminalSessionManager,
    taskKey: String,
    workspacePath: String,
    runtime: AgentRuntime,
    title: String,
): String? {
    val target = taskResumeTargets(sessions.listSessions(), taskKey).firstOrNull { it.runtime == runtime } ?: return null
    return startBuildTerminal(sessions, taskKey, workspacePath, title, target.command)
}
