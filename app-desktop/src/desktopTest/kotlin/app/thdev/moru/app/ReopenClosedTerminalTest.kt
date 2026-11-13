package app.thdev.moru.app

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.restore.ClosedTerminals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReopenClosedTerminalTest {
    @Test
    fun cmdWThenReopenBringsTheSameTerminalBackInItsTaskAndSelectsIt() {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(title = "zsh", workingDirectory = "/p", attachedItemId = "t1"))
        val claude = sessions.createSession(
            TerminalSessionSpec(title = "Claude", workingDirectory = "/p/wt", attachedItemId = "t1", startupCommand = "claude --session-id abc"),
        )
        sessions.selectSession(claude.id)
        var closed = emptyList<SavedTerminal>()
        val close = TerminalClose(sessions) { closed = ClosedTerminals.add(closed, it); closeTerminalNow(sessions, it.id) }

        close.request("t1")
        close.confirm()
        assertEquals(listOf("zsh"), sessions.listSessions().map { it.spec.title })

        val started = mutableListOf<TerminalSessionSnapshot>()
        val (remaining, reopened) = reopenClosedTerminal(closed, sessions, setOf("t1"), started::add)!!

        assertEquals(emptyList(), remaining)
        assertEquals(listOf(reopened), started)
        assertEquals(reopened.id, sessions.selectedSessionId())
        assertEquals("Claude", reopened.spec.title)
        assertEquals("/p/wt", reopened.spec.workingDirectory)
        assertEquals("t1", reopened.spec.attachedItemId)
        assertEquals("claude --resume abc", reopened.spec.startupCommand)
    }

    @Test
    fun aTerminalWhoseTaskIsGoneWaitsWhileAnOlderOneOfALiveTaskOpens() {
        val sessions = InMemoryTerminalSessionManager()
        val closed = listOf(SavedTerminal(title = "older", taskId = "t1"), SavedTerminal(title = "newer", taskId = "closed-project-task"))

        val (remaining, reopened) = reopenClosedTerminal(closed, sessions, setOf("t1")) {}!!

        assertEquals("older", reopened.spec.title)
        assertEquals(listOf("newer"), remaining.map { it.title })
    }

    @Test
    fun aFullTaskOrNothingClosedOpensNothing() {
        val sessions = InMemoryTerminalSessionManager()
        repeat(TaskTerminalSessionCapacity.Maximum) { sessions.createSession(TerminalSessionSpec(attachedItemId = "t1")) }

        assertNull(reopenClosedTerminal(listOf(SavedTerminal(taskId = "t1")), sessions, setOf("t1")) {})
        assertNull(reopenClosedTerminal(emptyList(), sessions, setOf("t1")) {})
        assertEquals(TaskTerminalSessionCapacity.Maximum, sessions.listSessions().size)
    }
}
