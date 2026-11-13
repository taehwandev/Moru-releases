package app.thdev.moru.feature.workbench.resume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.runtime.CliUpdates
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.feature.workbench.launch.AgentConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val POLL_MILLIS = 2_000L

/**
 * After Moru updates an agent CLI, moves every agent session of every Task that still runs the old version onto the
 * new one, as [cliUpdateRestartStep] decides: a quiet agent whose conversation is known is restarted into that
 * conversation, a busy one is left until its turn ends, and one whose conversation is unknown is put to the user
 * through [CliRestartRequests]. Each session is handled once per update. [activityOf] judges a session at a time,
 * given whether its shell runs a program.
 */
@Composable
fun RunCliUpdateRestarts(
    sessions: TerminalSessionManager,
    submitLine: (sessionKey: String, line: String) -> Boolean,
    activityOf: (snapshot: TerminalSessionSnapshot, nowMillis: Long, programRunning: Boolean?) -> SessionActivity,
) {
    val handled = remember(sessions) { mutableMapOf<String, Long>() }
    LaunchedEffect(sessions) {
        while (true) {
            delay(POLL_MILLIS)
            val now = System.currentTimeMillis()
            sessions.listSessions()
                .filter { it.spec.kind == TerminalSessionKind.TERMINAL && it.status == TerminalSessionStatus.READY }
                .forEach { session ->
                    val runtime = launchRuntimeOf(session.spec) ?: return@forEach
                    val updatedAt = CliUpdates.updatedAt(runtime.program) ?: return@forEach
                    if (handled[session.id] == updatedAt) return@forEach
                    val running = withContext(Dispatchers.Default) { SessionShellProcesses.hasRunningProgram(session.id) }
                    val activity = activityOf(session, now, running)
                    when (cliUpdateRestartStep(session, activity, running, updatedAt, now)) {
                        CliUpdateRestartStep.Wait -> Unit
                        CliUpdateRestartStep.Done -> handled[session.id] = updatedAt
                        CliUpdateRestartStep.AskUser -> {
                            handled[session.id] = updatedAt
                            CliRestartRequests.ask(session.id, runtime)
                        }
                        CliUpdateRestartStep.RestartNow -> {
                            handled[session.id] = updatedAt
                            val command = AgentConversation.resumeCommandFor(session.spec.startupCommand) ?: return@forEach
                            launch {
                                if (!restartAgentInSession(session.id, command, submitLine)) {
                                    CliRestartRequests.ask(session.id, runtime)
                                }
                            }
                        }
                    }
                }
            handled.keys.retainAll(sessions.listSessions().map { it.id }.toSet())
        }
    }
}
