package app.thdev.moru.feature.workbench.resume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.launch.AgentConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val POLL_MILLIS = 1_500L

/**
 * Watches the agent terminals of [taskKey]. It polls them, since a program ending does not change the session.
 */
@Composable
fun rememberAgentExits(
    sessionManager: TerminalSessionManager,
    taskKey: String,
): AgentWatch {
    val watcher = remember(sessionManager, taskKey) { AgentExitWatcher() }
    var exits by remember(sessionManager, taskKey) { mutableStateOf(emptyList<AgentExit>()) }
    var anyRunning by remember(sessionManager, taskKey) { mutableStateOf<Boolean?>(null) }
    // Reading shells scans the processes, so it repeats only while Moru's window has focus. An agent that ends in the
    // meantime is still seen ending on return, since the watcher remembers it running.
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(sessionManager, taskKey, focused) {
        while (true) {
            val taskAgents = sessionManager.listSessions().filter { session ->
                session.spec.kind == TerminalSessionKind.TERMINAL &&
                    session.spec.attachedItemId == taskKey &&
                    launchRuntimeOf(session.spec) != null
            }
            val resumeCommands = taskResumeTargets(taskAgents, taskKey).associate { it.runtime to it.command }
            val agents = taskAgents.filter { it.status == TerminalSessionStatus.READY }.mapNotNull { session ->
                launchRuntimeOf(session.spec)?.let { runtime ->
                    val command = AgentConversation.knownResumeCommandFor(session.spec.startupCommand)
                        ?.takeIf { it == resumeCommands[runtime] }
                    AgentExit(session.id, runtime, command)
                }
            }
            val readings = withContext(Dispatchers.Default) {
                agents.map { agent -> agent.sessionId to SessionShellProcesses.hasRunningProgram(agent.sessionId) }
            }
            watcher.retainOnly(agents.map { agent -> agent.sessionId }.toSet())
            readings.forEach { (id, running) -> watcher.observe(id, running) }
            val starting = taskAgents.filter { it.status == TerminalSessionStatus.STARTING }.map { null }
            anyRunning = agentRunningState(readings.map { it.second } + starting)
            val idleIds = readings.filter { it.second == false }.map { it.first }.toSet()
            exits = agents.filter { agent -> agent.sessionId in watcher.exitedSessions() && agent.sessionId in idleIds }
            if (!focused) break
            delay(POLL_MILLIS)
        }
    }
    return AgentWatch(exits, anyRunning) { sessionId ->
        watcher.acknowledge(sessionId)
        exits = exits.filterNot { exit -> exit.sessionId == sessionId }
    }
}
