package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.core.terminal.SessionResponseAttention
import app.thdev.moru.core.terminal.SessionBusyScreens
import app.thdev.moru.core.terminal.SessionQuestions
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentSessionRowsTest {
    private val now = 1_760_000_000_000L

    private fun session(
        id: String,
        task: String?,
        command: String? = "claude",
        lastOutputAgoMillis: Long? = 1_000,
        status: TerminalSessionStatus = TerminalSessionStatus.READY,
        exitCode: Int? = null,
    ) = TerminalSessionSnapshot(
        id = id,
        spec = TerminalSessionSpec(title = "T$id", attachedItemId = task, startupCommand = command),
        status = status,
        lastOutputAtMillis = lastOutputAgoMillis?.let { now - it },
        exitCode = exitCode,
    )

    @Test
    fun viewingAResponseDoesNotRearmItWhenTheTerminalRedrawsLater() {
        val id = "rows-delayed-redraw"
        val completed = session(id, "task-a", lastOutputAgoMillis = 60_000)
        fun activity(snapshot: TerminalSessionSnapshot, at: Long = now) =
            agentSessionRows(listOf(snapshot), id, at).single().activity
        try {
            assertEquals(SessionActivity.Quiet, activity(completed))
            SessionResponseAttention.acknowledge(id)
            assertEquals(SessionActivity.Waiting, activity(completed))
            val redrawn = completed.copy(lastOutputAtMillis = now)
            assertEquals(SessionActivity.Waiting, activity(redrawn))
            assertEquals(SessionActivity.Waiting, activity(redrawn, now + 9_000))

            SessionQuestions.record(id, "Allow command?")
            assertEquals(SessionActivity.Asking, activity(redrawn))
            SessionQuestions.forget(id)
            SessionBusyScreens.record(id, true)
            assertEquals(SessionActivity.Working, activity(redrawn))
            SessionBusyScreens.record(id, false)
            assertEquals(SessionActivity.Quiet, activity(redrawn, now + 9_000))
        } finally {
            SessionQuestions.forget(id)
            SessionBusyScreens.forget(id)
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun anUnreadResponseAlsoSurvivesPassiveOutputWhileAnOrdinaryTerminalKeepsItsActivity() {
        val id = "rows-unread-redraw"
        val completed = session(id, "task-a", lastOutputAgoMillis = 60_000)
        try {
            assertEquals(SessionActivity.Quiet, agentSessionRows(listOf(completed), null, now).single().activity)
            val redrawn = completed.copy(lastOutputAtMillis = now)
            assertEquals(SessionActivity.Quiet, agentSessionRows(listOf(redrawn), null, now).single().activity)
            kotlin.test.assertTrue(SessionResponseAttention.acknowledge(id))
            assertEquals(SessionActivity.Waiting, agentSessionRows(listOf(redrawn), null, now).single().activity)
            val ordinary = redrawn.copy(id = "rows-plain-redraw", spec = redrawn.spec.copy(startupCommand = null))
            assertEquals(SessionActivity.Working, agentSessionRows(listOf(ordinary), null, now).single().activity)
        } finally {
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun anUnseenNamedResponseStaysReadyUntilOpenedAndAnotherTurnRearmsIt() {
        val id = "rows-response-attention"
        val completed = session(id, "task-a", lastOutputAgoMillis = 6 * 60_000)
        fun activity() = agentSessionRows(listOf(completed), id, now).single().activity
        try {
            assertEquals(SessionActivity.Quiet, activity())
            SessionResponseAttention.acknowledge(id)
            assertEquals(SessionActivity.Waiting, activity())
            assertEquals(SessionActivity.Waiting, activity())
            SessionResponseAttention.observe(id, SessionActivity.Working)
            assertEquals(SessionActivity.Quiet, activity())
            val ordinary = completed.copy(id = "rows-ordinary", spec = completed.spec.copy(startupCommand = null))
            assertEquals(SessionActivity.Quiet, agentSessionRows(listOf(ordinary), null, now).single().activity)
        } finally {
            SessionResponseAttention.forget(id)
        }
    }

    @Test
    fun everySessionBecomesARowThatKnowsItsTaskItsAgentAndItsOwnState() {
        val rows = agentSessionRows(
            sessions = listOf(
                session("1", "task-a"),
                session("2", null, command = null, status = TerminalSessionStatus.EXITED, exitCode = 0),
            ),
            selectedSessionId = "1",
            nowMillis = now,
            lastLineOf = { id -> "line $id" },
            questionOf = { id -> if (id == "1") "Allow edit?" else null },
        )

        assertEquals(listOf("Allow edit?", null), rows.map { row -> row.question })
        assertEquals(listOf("task-a", null), rows.map { row -> row.taskKey })
        assertEquals(listOf(AgentRuntime.Named("Claude"), AgentRuntime.Terminal), rows.map { row -> row.agent })
        assertEquals(listOf(SessionActivity.Working, SessionActivity.Exited), rows.map { row -> row.activity })
        assertEquals(listOf(true, false), rows.map { row -> row.selected })
        assertEquals(listOf("line 1", "line 2"), rows.map { row -> row.lastLine })
        // A resumed agent started with options before its subcommand is still named by its program.
        assertEquals(AgentRuntime.Named("Codex"), agentRuntimeOf(TerminalSessionSpec(startupCommand = "codex --no-daemon resume x9")))
    }

    @Test
    fun theAgentRunningNowOutranksTheOneTheSessionStartedWith() {
        val rows = agentSessionRows(
            sessions = listOf(session("1", null), session("2", null, command = null), session("3", null)),
            selectedSessionId = null,
            nowMillis = now,
            runningAgentOf = { id -> mapOf("1" to "codex", "2" to "agy")[id] },
        )

        // Session 3 runs no agent now (its Claude quit, or it has not been read yet), so it keeps its startup command.
        assertEquals(
            listOf(AgentRuntime.Named("Codex"), AgentRuntime.Named("AGY"), AgentRuntime.Named("Claude")),
            rows.map { row -> row.agent },
        )
    }

    @Test
    fun theListOfEverySessionPutsWhatNeedsALookFirstThenWhatIsWorking() {
        val rows = agentSessionRows(
            sessions = listOf(
                session("working", "a", lastOutputAgoMillis = 1_000),
                session("exited", "a", status = TerminalSessionStatus.EXITED, exitCode = 0),
                session("also-working", "b", lastOutputAgoMillis = 2_000),
                session("failed", "b", status = TerminalSessionStatus.EXITED, exitCode = 1),
            ),
            selectedSessionId = null,
            nowMillis = now,
        )

        val ordered = rows.byUrgency().map { row -> row.sessionId }

        // An exit says nothing about whether the goal was met, so both exits wait for the user; rows of one weight keep their order.
        assertEquals(listOf("exited", "failed", "working", "also-working"), ordered)
    }
}
