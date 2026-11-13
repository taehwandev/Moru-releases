package app.thdev.moru.app

import app.thdev.moru.core.terminal.TerminalSessionManager

/** A Task's terminals in tab order. */
internal fun taskTerminalIds(sessions: TerminalSessionManager, taskId: String?): List<String> =
    if (taskId == null) emptyList()
    else sessions.listSessions()
        .filter { session -> session.spec.kind.isTaskSession && session.spec.attachedItemId == taskId }
        .map { session -> session.id }

/**
 * The terminal [step] tabs after the selected one in [taskId], wrapping round the ends; from no selection it starts
 * at the first or last tab. Null when the Task has no terminal.
 */
internal fun adjacentTaskSession(sessions: TerminalSessionManager, taskId: String?, step: Int): String? {
    val ids = taskTerminalIds(sessions, taskId)
    if (ids.isEmpty()) return null
    val current = ids.indexOf(sessions.selectedSessionId())
    val target = if (current < 0) {
        if (step >= 0) 0 else ids.lastIndex
    } else {
        Math.floorMod(current + step, ids.size)
    }
    return ids[target]
}
