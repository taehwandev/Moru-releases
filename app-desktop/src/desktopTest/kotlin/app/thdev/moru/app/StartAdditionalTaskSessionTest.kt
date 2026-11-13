package app.thdev.moru.app

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.LimitReached
import app.thdev.moru.feature.workbench.launch.AgentConversation
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StartAdditionalTaskSessionTest {
    @Test
    fun aRequestPastTheMaximumNeverStartsAPtyOrChangesExistingSessionsOrSelection() {
        val sessions = InMemoryTerminalSessionManager(List(TaskTerminalSessionCapacity.Maximum - 1) { TerminalSessionSpec(attachedItemId = "task-a") })
        var starts = 0
        startAdditionalTaskSession(sessions, "task-a", "/folder", "/bin/sh", AgentRuntime.Terminal) { starts++ }.getOrThrow()
        sessions.selectSession(sessions.listSessions().first().id)
        val before = sessions.listSessions()
        val selected = sessions.selectedSessionId()
        val revision = sessions.revision()
        val rejection = startAdditionalTaskSession(sessions, "task-a", "/folder", "/bin/sh", AgentRuntime.Codex) { starts++ }
        assertTrue(rejection.exceptionOrNull() is LimitReached)
        assertEquals(1, starts, "Rejected admission must not call the native terminal starter")
        assertEquals(before, sessions.listSessions())
        assertEquals(selected, sessions.selectedSessionId())
        assertEquals(revision, sessions.revision())
    }

    @Test
    fun everyRuntimeStartsInTheCapturedTaskAndFolderAndBecomesSelected() {
        val sessions = InMemoryTerminalSessionManager()
        val other = sessions.createSession(TerminalSessionSpec(attachedItemId = "other-task"))
        val commands = mapOf(
            AgentRuntime.Claude to "claude",
            AgentRuntime.Codex to "codex --no-daemon",
            AgentRuntime.Agy to "agy",
            AgentRuntime.Terminal to null,
        )
        AgentRuntime.entries.forEach { runtime ->
            var started = false
            val added = startAdditionalTaskSession(sessions, "task-a", "/project folder", "/bin/sh", runtime) {
                assertEquals("task-a", it.spec.attachedItemId)
                assertEquals("/project folder", it.spec.workingDirectory)
                assertEquals("/bin/sh", it.spec.shell)
                // Each Claude terminal starts its own conversation, so its command carries a fresh id.
                assertEquals(commands[runtime], it.spec.startupCommand?.replace(Regex(" --session-id \\S+"), ""))
                if (runtime == AgentRuntime.Claude) assertTrue(AgentConversation.idIn(it.spec.startupCommand) != null)
                started = true
            }.getOrThrow()
            assertTrue(started)
            assertEquals(runtime.displayName, added.title)
            assertEquals(added.id, sessions.selectedSessionId())
        }
        assertEquals(other, sessions.listSessions().first())
        assertEquals(5, sessions.listSessions().size)
    }

    @Test
    fun startupFailureRemovesOnlyTheAddedSessionAndRestoresThePreviousSelection() {
        val sessions = InMemoryTerminalSessionManager()
        val first = sessions.createSession(TerminalSessionSpec(attachedItemId = "task-a"))
        sessions.createSession(TerminalSessionSpec(attachedItemId = "task-a"))
        sessions.selectSession(first.id)
        val before = sessions.listSessions()
        val error = IllegalStateException("The shell could not start")
        val failed = startAdditionalTaskSession(sessions, "task-a", "/folder", "/bin/sh", AgentRuntime.Codex) { throw error }
        assertSame(error, failed.exceptionOrNull())
        assertEquals(before, sessions.listSessions())
        assertEquals(first.id, sessions.selectedSessionId())
        assertTrue(startAdditionalTaskSession(sessions, "task-a", "/folder", "/bin/sh", AgentRuntime.Terminal) {}.isSuccess)
        assertEquals(3, sessions.listSessions().size)
    }
}
