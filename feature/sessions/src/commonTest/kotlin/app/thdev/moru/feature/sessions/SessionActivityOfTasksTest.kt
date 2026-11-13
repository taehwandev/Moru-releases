package app.thdev.moru.feature.sessions

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** How the sessions module reads the activity core:terminal derives: per Task, and whether it needs attention. */
class SessionActivityOfTasksTest {
    private val now = 1_000_000L

    private fun snapshot(lastOutputAtMillis: Long? = null) = TerminalSessionSnapshot(
        id = "term-1",
        spec = TerminalSessionSpec(title = "claude"),
        status = TerminalSessionStatus.READY,
        lastOutputAtMillis = lastOutputAtMillis,
    )

    @Test
    fun aShellAtItsPromptNeedsNoAttention() {
        assertEquals(false, SessionActivity.Idle.presentation().needsAttention)
    }

    @Test
    fun aTaskIsOnlyQuietAfterTheLongerWindowSoItDoesNotFlipOnEveryPause() {
        val tenSecondsAgo = snapshot(lastOutputAtMillis = now - 10_000)
        val aMinuteAgo = snapshot(lastOutputAtMillis = now - 60_000)
        val task = { session: TerminalSessionSnapshot ->
            taskSessionActivities(
                listOf(session.copy(spec = session.spec.copy(attachedItemId = "task-1"))),
                now,
                hasRunningProgram = { true },
            ).getValue("task-1")
        }

        assertEquals(SessionActivity.Working, task(tenSecondsAgo))
        assertEquals(SessionActivity.Quiet, task(aMinuteAgo))
    }

}
