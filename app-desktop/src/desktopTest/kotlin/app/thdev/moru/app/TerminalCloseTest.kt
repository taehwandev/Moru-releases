package app.thdev.moru.app

import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalCloseTest {
    private fun sessionsWith(vararg tasks: String): Pair<InMemoryTerminalSessionManager, List<String>> {
        val sessions = InMemoryTerminalSessionManager()
        val ids = tasks.map { task -> sessions.createSession(TerminalSessionSpec(title = task, attachedItemId = task)).id }
        return sessions to ids
    }

    @Test
    fun askingChangesNothingUntilTheUserConfirms() {
        val (sessions, ids) = sessionsWith("task-1")
        val close = TerminalClose(sessions)

        close.request("task-1")

        assertEquals(ids.single(), close.pendingSessionId)
        assertEquals(1, sessions.listSessions().size)
    }

    @Test
    fun confirmingClosesOnlyTheSelectedTerminalOfTheTask() {
        val (sessions, ids) = sessionsWith("task-1", "task-1", "task-2")
        sessions.selectSession(ids[1])
        val close = TerminalClose(sessions)

        close.request("task-1")
        close.confirm()

        assertEquals(listOf(ids[0], ids[2]), sessions.listSessions().map { it.id })
        assertNull(close.pendingSessionId)
    }

    @Test
    fun aTaskWhoseSelectedTerminalBelongsToAnotherTaskClosesItsOwnFirst() {
        val (sessions, ids) = sessionsWith("task-1", "task-2")
        sessions.selectSession(ids[1])
        val close = TerminalClose(sessions)

        close.request("task-1")
        close.confirm()

        assertEquals(listOf(ids[1]), sessions.listSessions().map { it.id })
    }

    @Test
    fun askingTwiceForTheSameLabClosesIt() {
        val sessions = InMemoryTerminalSessionManager()
        val lab = sessions.createSession(TerminalSessionSpec(title = "Claude Lab", kind = TerminalSessionKind.LAB, attachedItemId = "task-1")).id
        val close = TerminalClose(sessions)

        close.request("task-1")
        close.request("task-1")

        assertTrue(sessions.listSessions().none { it.id == lab })
        assertNull(close.pendingSessionId)
    }

    @Test
    fun askingAgainAfterSwitchingTerminalsOnlyAsksAboutTheNewOne() {
        val (sessions, ids) = sessionsWith("task-1", "task-1")
        val close = TerminalClose(sessions)

        sessions.selectSession(ids[0])
        close.request("task-1")
        sessions.selectSession(ids[1])
        close.request("task-1")

        assertEquals(ids[1], close.pendingSessionId)
        assertEquals(2, sessions.listSessions().size)
    }

    @Test
    fun keepingLeavesEveryTerminalRunning() {
        val (sessions, _) = sessionsWith("task-1")
        val close = TerminalClose(sessions)

        close.request("task-1")
        close.keep()
        close.confirm()

        assertEquals(1, sessions.listSessions().size)
        assertNull(close.pendingSessionId)
    }

    @Test
    fun aTaskWithoutTerminalsHasNothingToClose() {
        val (sessions, _) = sessionsWith("task-2")
        val close = TerminalClose(sessions)

        assertFalse(close.hasTerminal("task-1"))
        assertFalse(close.hasTerminal(null))
        assertTrue(close.hasTerminal("task-2"))
        close.request("task-1")
        assertNull(close.pendingSessionId)
    }
}
