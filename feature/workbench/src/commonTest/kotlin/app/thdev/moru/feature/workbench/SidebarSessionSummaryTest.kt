package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.feature.workbench.sidebar.SidebarSessionSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarSessionSummaryTest {
    @Test
    fun allObservedStatesCountOnceAndQuietProgramsStayRunning() {
        val rows = SessionActivity.entries.map { row(it.name, it) } +
            row("server", SessionActivity.Quiet, AgentRuntime.Terminal)
        val summary = SidebarSessionSummary.from(rows)
        assertEquals(SidebarSessionSummary(9, running = 3, needsAttention = 2, responsesReady = 1, waiting = 2, ended = 1), summary)
        assertEquals(summary.total, summary.running + summary.needsAttention + summary.responsesReady + summary.waiting + summary.ended)
        // A finished turn that asks the user is counted apart from a finished report.
        val asked = SidebarSessionSummary.from(rows + row("asked", SessionActivity.Quiet).copy(replyQuestion = "Ship it?"))
        assertEquals(1, asked.awaitingReply)
        assertEquals(1, asked.responsesReady)
    }

    @Test
    fun livePermissionsOverrideBothRunningAndFinishedResponses() {
        val rows = listOf(row("working", SessionActivity.Working), row("done", SessionActivity.Quiet),
            row("idle", SessionActivity.Idle))
        assertEquals(SidebarSessionSummary(3, needsAttention = 3),
            SidebarSessionSummary.from(rows, setOf("working", "done", "idle")))
    }

    @Test
    fun acknowledgedWaitingSessionsNoLongerCountAsFinishedResponses() {
        assertEquals(SidebarSessionSummary(total = 1, waiting = 1),
            SidebarSessionSummary.from(listOf(row("seen", SessionActivity.Waiting))))
    }

    @Test
    fun persistedTaskDecisionsRemainVisibleAlongsideIdleSessionsWithoutDuplicatingSessionAttention() {
        val task = TaskRow("task", "project", "Moru", "Fix login", TaskDisplayState.NeedsApproval,
            TaskGroup.NeedsAttention, TaskAttention.None, true, false)
        val idle = row("session", SessionActivity.Idle).copy(taskKey = task.id)
        assertEquals(1, SidebarSessionSummary.unrepresentedTaskAttention(listOf(task), listOf(idle), emptySet()))
        assertEquals(0, SidebarSessionSummary.unrepresentedTaskAttention(listOf(task), listOf(idle), setOf(idle.sessionId)))
        assertEquals(0, SidebarSessionSummary.unrepresentedTaskAttention(
            listOf(task), listOf(idle.copy(activity = SessionActivity.Asking)), emptySet()))
        assertEquals(0, SidebarSessionSummary.unrepresentedTaskAttention(
            listOf(task.copy(displayState = TaskDisplayState.Done)), listOf(idle.copy(activity = SessionActivity.Quiet)), emptySet()))
    }

    private fun row(id: String, activity: SessionActivity, agent: AgentRuntime = AgentRuntime.Named("Codex")) =
        AgentSessionRow(id, "Work", agent, activity, null, 0, null, null, false)
}
