package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeRestartPolicyTest {
    private fun session(id: String, task: String = "task", command: String = "codex --no-daemon resume c-$id") =
        TerminalSessionSnapshot(
            id, TerminalSessionSpec(title = "Session $id", attachedItemId = task, startupCommand = command),
            TerminalSessionStatus.READY,
        )

    @Test
    fun theSelectedStaleCodexWinsWithinItsOwningTask() {
        val first = session("first")
        val second = session("second")
        val otherTask = session("other", task = "other-task")
        assertEquals(second, themeRestartTarget(listOf(first, second, otherTask), "task", "second") { true })
        assertEquals(first, themeRestartTarget(listOf(first, second, otherTask), "task", "other") { true })
        assertEquals(first, themeRestartTarget(listOf(first, second), "task", "second") { it == "first" })
        assertNull(themeRestartTarget(listOf(otherTask), "task", "other") { true })
    }

    @Test
    fun nonCodexPreviewAndClosedSessionsNeverShowTheNotice() {
        val codex = session("codex")
        val others = listOf(
            session("claude", command = "claude --session-id c1"),
            session("shell", command = ""),
            codex.copy(spec = codex.spec.copy(kind = TerminalSessionKind.WEB_PREVIEW)),
            codex.copy(status = TerminalSessionStatus.EXITED),
            codex.copy(status = TerminalSessionStatus.FAILED),
        )
        assertNull(themeRestartTarget(others, "task", null) { true })
        assertNull(themeRestartTarget(listOf(codex), "task", "codex") { false })
    }

    @Test
    fun workingAskingStartingAndAnOutstandingRequestDisableRestart() {
        val codex = session("codex")
        for (activity in listOf(SessionActivity.Working, SessionActivity.Asking, SessionActivity.Starting, SessionActivity.Failed, SessionActivity.Exited)) {
            assertFalse(themeRestartAllowed(codex, activity), "$activity must not interrupt Codex")
        }
        assertFalse(themeRestartAllowed(codex.copy(status = TerminalSessionStatus.STARTING), SessionActivity.Idle))
        assertFalse(themeRestartAllowed(codex, SessionActivity.Waiting, requestInProgress = true))
        assertTrue(themeRestartAllowed(codex, SessionActivity.Waiting))
        assertTrue(themeRestartAllowed(codex, SessionActivity.Quiet))
    }

    @Test
    fun theClickGuardRejectsAnOwnerRuntimeOrActivityThatChanged() {
        val codex = session("codex")
        fun allowed(current: TerminalSessionSnapshot, activity: SessionActivity = SessionActivity.Waiting, stale: Boolean = true) =
            themeRestartGuard(current, "task", "codex", 1_000L, { _, _ -> activity }, { stale })
        assertTrue(allowed(codex))
        assertFalse(allowed(codex.copy(id = "different")))
        assertFalse(allowed(codex.copy(spec = codex.spec.copy(attachedItemId = "different-task"))))
        assertFalse(allowed(codex.copy(spec = codex.spec.copy(startupCommand = "claude"))))
        assertFalse(allowed(codex, SessionActivity.Working))
        assertFalse(allowed(codex, SessionActivity.Asking))
        assertFalse(allowed(codex, stale = false))
    }
}
