package app.thdev.moru.app

import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.restore.ClosedTerminals
import app.thdev.moru.feature.workbench.restore.savedTerminalSpec

/**
 * Opens the newest terminal in [closed] whose Task is still in [liveTaskIds] and has room, starts it and selects it.
 * Returns the closed list without it and the new terminal, or null when none can open. A closed terminal whose Task is
 * gone for now (its Project closed) stays in the list for when the Task comes back.
 */
internal fun reopenClosedTerminal(
    closed: List<SavedTerminal>,
    sessions: TerminalSessionManager,
    liveTaskIds: Set<String>,
    startTerminal: (TerminalSessionSnapshot) -> Unit,
): Pair<List<SavedTerminal>, TerminalSessionSnapshot>? {
    val index = ClosedTerminals.newestOpenable(closed) { taskId ->
        taskId in liveTaskIds && sessions.canAddTaskTerminalSession(taskId)
    } ?: return null
    val session = runCatching { sessions.createTaskTerminalSession(savedTerminalSpec(closed[index])) }.getOrNull() ?: return null
    // A Lab runs nothing until its screen sends a message; a terminal starts its shell and the agent's resume.
    if (session.spec.kind != TerminalSessionKind.LAB) runCatching { startTerminal(session) }
    sessions.selectSession(session.id)
    return closed.filterIndexed { at, _ -> at != index } to session
}
