package app.thdev.moru.feature.sessions

import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/**
 * The line that says what each Task's terminal is doing now. A Task with several sessions shows the line of
 * the one that printed most recently, since that is the one that moved; a session that printed nothing
 * meaningful yet is skipped.
 */
fun taskLastLines(
    snapshots: List<TerminalSessionSnapshot>,
    lineOf: (sessionId: String) -> String?,
): Map<String, String> =
    snapshots
        .mapNotNull { snapshot ->
            val taskKey = snapshot.spec.attachedItemId ?: return@mapNotNull null
            val line = lineOf(snapshot.id) ?: return@mapNotNull null
            Triple(taskKey, snapshot.lastOutputAtMillis ?: 0L, line)
        }
        .groupBy { (taskKey, _, _) -> taskKey }
        .mapValues { (_, lines) -> lines.maxBy { (_, at, _) -> at }.third }
