package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.SessionShellProcesses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val STOP_POLL_MILLIS = 300L
private const val STOP_PATIENCE_MILLIS = 10_000L

/**
 * Restarts the agent in [sessionId] in the same terminal: asks it to quit, waits for the shell to be back at its
 * prompt, then types [command] (the line that resumes its conversation). Nothing is typed unless the agent really
 * quit, so a line never lands in a running agent. Returns whether the line was typed.
 */
suspend fun restartAgentInSession(
    sessionId: String,
    command: String,
    submitLine: (sessionKey: String, line: String) -> Boolean,
    stopProgram: (sessionId: String) -> Boolean = SessionShellProcesses::stopRunningProgram,
    programRunning: (sessionId: String) -> Boolean? = SessionShellProcesses::hasRunningProgram,
): Boolean {
    if (!withContext(Dispatchers.Default) { stopProgram(sessionId) }) return false
    var waited = 0L
    while (waited < STOP_PATIENCE_MILLIS) {
        delay(STOP_POLL_MILLIS)
        waited += STOP_POLL_MILLIS
        if (withContext(Dispatchers.Default) { programRunning(sessionId) } == false) return submitLine(sessionId, command)
    }
    return false
}
