package app.thdev.moru.feature.workbench.sidebar

import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.feature.workbench.TaskRow

/** Session observations, never a claim that the owning Task has completed. */
internal data class SidebarSessionSummary(
    val total: Int = 0,
    val running: Int = 0,
    val needsAttention: Int = 0,
    val responsesReady: Int = 0,
    /** Finished turns that ended by asking the user; not counted in [responsesReady]. */
    val awaitingReply: Int = 0,
    val waiting: Int = 0,
    val ended: Int = 0,
) {
    val hasActivity: Boolean get() = running + needsAttention + responsesReady + awaitingReply > 0

    companion object {
        fun from(rows: List<AgentSessionRow>, permissionSessionIds: Set<String> = emptySet()): SidebarSessionSummary {
            var running = 0
            var attention = 0
            var ready = 0
            var reply = 0
            var waiting = 0
            var ended = 0
            rows.forEach { row ->
                when {
                    row.sessionId in permissionSessionIds -> attention++
                    row.activity == SessionActivity.Asking || row.activity == SessionActivity.Failed -> attention++
                    row.awaitsReply -> reply++
                    row.finishedTurn -> ready++
                    row.activity in RunningActivities -> running++
                    row.activity == SessionActivity.Waiting || row.activity == SessionActivity.Idle -> waiting++
                    row.activity == SessionActivity.Exited -> ended++
                }
            }
            return SidebarSessionSummary(rows.size, running, attention, ready, awaitingReply = reply, waiting = waiting, ended = ended)
        }

        /** Keep durable Task decisions visible without counting the same observed session attention twice. */
        fun unrepresentedTaskAttention(tasks: List<TaskRow>, rows: List<AgentSessionRow>, permissions: Set<String>): Int =
            tasks.count { task ->
                task.group == TaskGroup.NeedsAttention && task.displayState != TaskDisplayState.Done &&
                    rows.none { row -> row.taskKey == task.id &&
                        (row.sessionId in permissions || row.activity == SessionActivity.Asking || row.activity == SessionActivity.Failed) }
            }

        private val RunningActivities = setOf(SessionActivity.Starting, SessionActivity.Working, SessionActivity.Quiet)
    }
}
