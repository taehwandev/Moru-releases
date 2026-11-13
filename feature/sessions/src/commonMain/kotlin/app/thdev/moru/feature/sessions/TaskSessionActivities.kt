package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.SessionRunningAgents
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.deriveSessionActivity

/**
 * A Task is only judged quiet after this much silence, the same bar the notifications use. A shorter window made
 * every few seconds without output flip the Task between Working and "Needs you" and back.
 */
const val TASK_QUIET_AFTER_MILLIS: Long = 45_000

/**
 * The one activity that speaks for each Task, judged from the sessions attached to it.
 * When a Task has several sessions the one that most needs the user wins, so a failed or
 * quiet session is never hidden behind a busy one.
 */
fun taskSessionActivities(
    snapshots: List<TerminalSessionSnapshot>,
    nowMillis: Long,
    quietAfterMillis: Long = TASK_QUIET_AFTER_MILLIS,
    hasRunningProgram: (sessionId: String) -> Boolean? = SessionShellProcesses::observedRunningProgram,
    runningAgentOf: (sessionId: String) -> String? = SessionRunningAgents::agentFor,
): Map<String, SessionActivity> =
    snapshots
        .mapNotNull { snapshot ->
            snapshot.spec.attachedItemId?.let { taskKey ->
                val agent = runningAgentOf(snapshot.id)?.let(::agentRuntimeOfProgram) ?: agentRuntimeOf(snapshot.spec)
                val observed = deriveSessionActivity(snapshot, nowMillis, quietAfterMillis, hasRunningProgram)
                val activity = if (agent is AgentRuntime.Named &&
                    (observed == SessionActivity.Quiet || SessionResponseAttention.isAcknowledged(snapshot.id))
                ) {
                    SessionResponseAttention.project(snapshot.id, observed)
                } else {
                    observed
                }
                taskKey to activity
            }
        }
        .groupBy({ (taskKey, _) -> taskKey }, { (_, activity) -> activity })
        .mapValues { (_, activities) -> activities.minBy { activity -> activity.attentionRank() } }

/** Lower ranks win. Failed first, then an agent asking a question, then a quiet one that may be waiting, then the rest. */
private fun SessionActivity.attentionRank(): Int =
    when (this) {
        SessionActivity.Failed -> 0
        SessionActivity.Asking -> 1
        SessionActivity.Quiet -> 2
        SessionActivity.Working -> 3
        SessionActivity.Starting -> 4
        SessionActivity.Waiting -> 5
        SessionActivity.Idle -> 6
        SessionActivity.Exited -> 7
    }
