package app.thdev.moru.app

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionQuestions
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionPermissionRequest
import app.thdev.moru.core.terminal.SessionTitles
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.deriveSessionActivity
import app.thdev.moru.feature.workbench.resume.launchRuntimeOf

/**
 * The Tasks that need the user right now. A failed terminal always does, then one whose screen asks a question
 * ([questionOf]), at once and whatever program asks it. An agent that stopped printing has finished
 * its turn, even while another agent in the same Task still works: each agent is judged on its own, and the one
 * silent longest is named. Claude and Codex redraw a spinner while they think, so silence past the working window
 * is the end of a turn, not a pause in it. Native Labs report their structured turn and background-work state
 * instead of relying on output timing. Only an agent counts: a plain terminal sitting at its prompt is idle, not
 * waiting. [titles] maps a Task id to its title; a Task without one is not listed. [summaryOf] gives what a finished
 * agent says it worked on (the terminal title it sets), so the user can tell which work ended.
 */
internal fun attentionCandidates(
    titles: Map<String, String>,
    sessions: List<TerminalSessionSnapshot>,
    nowMillis: Long,
    questionOf: (sessionId: String) -> String? = SessionQuestions::questionFor,
    permissions: Map<String, SessionPermissionRequest> = SessionPermissions.requests.value,
    summaryOf: (sessionId: String) -> String? = SessionTitles::titleFor,
): List<AttentionCandidate> =
    sessions
        .filter { session -> session.spec.kind.isTaskSession || session.id in permissions }
        .groupBy { session -> session.spec.attachedItemId }
        .flatMap { (taskId, group) ->
            val title = taskId?.let(titles::get) ?: return@flatMap emptyList()
            val requests = group.mapNotNull { asking ->
                val request = permissions[asking.id] ?: return@mapNotNull null
                AttentionCandidate(taskId, title, AttentionKind.Asking,
                    agent = launchRuntimeOf(asking.spec)?.displayName, question = request.question,
                    sessionId = asking.id, permissionRequestId = request.id)
            }
            if (requests.isNotEmpty()) return@flatMap requests
            val activities = group.associateWith { session ->
                val observed = deriveSessionActivity(session, nowMillis, asksUser = { id -> questionOf(id) != null })
                if (launchRuntimeOf(session.spec) != null) SessionResponseAttention.project(session.id, observed) else observed
            }
            activities.entries.firstOrNull { (_, activity) -> activity == SessionActivity.Failed }?.key?.let { failed ->
                return@flatMap listOf(AttentionCandidate(taskId, title, AttentionKind.Failed, sessionId = failed.id))
            }
            activities.entries.firstOrNull { (_, activity) -> activity == SessionActivity.Asking }?.key?.let { asking ->
                return@flatMap listOf(AttentionCandidate(
                    taskId, title, AttentionKind.Asking,
                    agent = launchRuntimeOf(asking.spec)?.displayName, question = questionOf(asking.id), sessionId = asking.id,
                ))
            }
            val finished = activities
                .filter { (session, activity) -> activity == SessionActivity.Quiet && launchRuntimeOf(session.spec) != null }
                .keys
                .minByOrNull { session -> session.lastOutputAtMillis ?: Long.MAX_VALUE }
                ?: return@flatMap emptyList()
            listOf(AttentionCandidate(
                taskId, title, AttentionKind.Finished,
                agent = launchRuntimeOf(finished.spec)?.displayName, sessionId = finished.id, summary = summaryOf(finished.id),
            ))
        }
