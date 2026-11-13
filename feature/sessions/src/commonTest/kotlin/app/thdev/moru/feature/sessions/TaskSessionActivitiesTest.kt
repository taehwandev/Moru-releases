package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The one activity each Task shows, judged from its sessions; split from the workbench's Task overlay tests. */
class TaskSessionActivitiesTest {
    @Test
    fun severalSessionsOnOneTaskReportTheOneThatNeedsTheUserMost() {
        val now = 1_000_000L

        fun session(id: String, status: TerminalSessionStatus, lastOutput: Long?, exitCode: Int? = null) =
            TerminalSessionSnapshot(
                id = id,
                spec = TerminalSessionSpec(title = id, attachedItemId = "task-1"),
                status = status,
                lastOutputAtMillis = lastOutput,
                exitCode = exitCode,
            )

        val activities = taskSessionActivities(
            listOf(
                session("busy", TerminalSessionStatus.READY, now - 1_000),
                session("quiet", TerminalSessionStatus.READY, now - 60_000),
            ),
            now,
        )
        assertEquals(SessionActivity.Quiet, activities.getValue("task-1"))

        val withFailure = taskSessionActivities(
            listOf(
                session("quiet", TerminalSessionStatus.READY, now - 60_000),
                session("crashed", TerminalSessionStatus.EXITED, now - 5_000, exitCode = 1),
            ),
            now,
        )
        assertEquals(SessionActivity.Failed, withFailure.getValue("task-1"))
    }

    @Test
    fun sessionsNotAttachedToATaskAreIgnored() {
        val detached = TerminalSessionSnapshot(
            id = "free",
            spec = TerminalSessionSpec(title = "free"),
            status = TerminalSessionStatus.READY,
        )

        assertEquals(emptyMap(), taskSessionActivities(listOf(detached), 0L))
    }

    @Test
    fun acknowledgedAgentResponseKeepsTaskWaitingThroughTaskQuietThresholdAndRedraws() {
        val id = "acknowledged-task-agent"
        fun snapshot(lastOutput: Long) = TerminalSessionSnapshot(
            id = id,
            spec = TerminalSessionSpec(title = "Claude", startupCommand = "claude", attachedItemId = "task-1"),
            status = TerminalSessionStatus.READY,
            lastOutputAtMillis = lastOutput,
        )
        try {
            assertEquals(SessionActivity.Quiet, SessionResponseAttention.project(id, SessionActivity.Quiet))
            assertTrue(SessionResponseAttention.acknowledge(id))

            // Acknowledged between the 8s session window and the 45s task window: Task is immediately Waiting.
            assertEquals(
                SessionActivity.Waiting,
                taskSessionActivities(listOf(snapshot(lastOutput = 10_000L)), nowMillis = 22_000L, hasRunningProgram = { true }).getValue("task-1"),
            )
            // Crossing the 45s task threshold must not flip the Task back to Quiet (Done).
            assertEquals(
                SessionActivity.Waiting,
                taskSessionActivities(listOf(snapshot(lastOutput = 10_000L)), nowMillis = 70_000L, hasRunningProgram = { true }).getValue("task-1"),
            )
            // Passive redraw output without a new turn must also stay Waiting.
            assertEquals(
                SessionActivity.Waiting,
                taskSessionActivities(listOf(snapshot(lastOutput = 75_000L)), nowMillis = 76_000L, hasRunningProgram = { true }).getValue("task-1"),
            )

            // Actual new turn rearms Working -> Quiet.
            SessionResponseAttention.observe(id, SessionActivity.Working)
            assertEquals(
                SessionActivity.Working,
                taskSessionActivities(listOf(snapshot(lastOutput = 80_000L)), nowMillis = 90_000L, hasRunningProgram = { true }).getValue("task-1"),
            )
            assertEquals(
                SessionActivity.Quiet,
                taskSessionActivities(listOf(snapshot(lastOutput = 80_000L)), nowMillis = 130_000L, hasRunningProgram = { true }).getValue("task-1"),
            )
        } finally {
            SessionResponseAttention.forget(id)
        }
    }
}
