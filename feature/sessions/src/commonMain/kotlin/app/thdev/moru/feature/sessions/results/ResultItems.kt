package app.thdev.moru.feature.sessions.results

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.feature.sessions.SessionBoardSection
import app.thdev.moru.feature.sessions.boardSection

/**
 * The sessions the Results view lists, sorted the way the status board judges them: what needs the user first, newest
 * first, then finished turns, newest first, including ones already opened. Running sessions and those with nothing to
 * report are left out; the view is about outcomes.
 */
internal fun resultItems(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    permissionSessions: Set<String>,
    results: Map<String, String>,
): List<ResultItem> = rows.mapNotNull { row ->
    val hasPermission = row.sessionId in permissionSessions
    val kind = when (row.boardSection(hasPermission)) {
        SessionBoardSection.NeedsAttention -> when {
            hasPermission -> ResultKind.Permission
            row.activity == SessionActivity.Failed -> ResultKind.Failed
            else -> ResultKind.Question
        }
        SessionBoardSection.AwaitingReply -> ResultKind.Question
        SessionBoardSection.Completed -> ResultKind.Done
        // Opening a finished response moves it to Ready, but its result stays here until the user marks it seen.
        SessionBoardSection.Ready -> ResultKind.Done.takeIf { row.agent is AgentRuntime.Named && row.sessionId in results }
        SessionBoardSection.Running -> null
    } ?: return@mapNotNull null
    ResultItem(row, tasks[row.taskKey], results[row.sessionId], kind)
}.sortedByDescending { it.row.lastOutputAtMillis ?: 0L }
