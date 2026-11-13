package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StartResumeTerminalTest {
    @Test
    fun resumingOpensANewTerminalOfTheTaskInItsFolderOnItsOwnConversation() {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(title = "Claude", attachedItemId = "task-1", startupCommand = "claude --session-id c1 'fix it'"))

        val id = startResumeTerminal(sessions, "task-1", "/work/alpha", AgentRuntime.Claude, "Claude · resumed")

        val session = sessions.listSessions().last()
        assertEquals(id, session.id)
        assertEquals("claude --resume c1", session.spec.startupCommand)
        assertEquals("task-1", session.spec.attachedItemId)
        assertEquals("/work/alpha", session.spec.workingDirectory)
        assertEquals("Claude · resumed", session.spec.title)
        assertEquals(id, sessions.selectedSessionId())
    }

    @Test
    fun aTaskNeverResumesAnotherTasksConversationInTheSameFolder() {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(title = "Claude", workingDirectory = "/work/alpha", attachedItemId = "task-1", startupCommand = "claude --session-id mine"))
        // Another Task in the same folder used Claude after this one.
        sessions.createSession(TerminalSessionSpec(title = "Claude", workingDirectory = "/work/alpha", attachedItemId = "task-2", startupCommand = "claude --session-id theirs"))

        startResumeTerminal(sessions, "task-1", "/work/alpha", AgentRuntime.Claude, "Claude · resumed")

        assertEquals("claude --resume mine", sessions.listSessions().last().spec.startupCommand)
    }

    @Test
    fun withoutAKnownTaskConversationResumeDoesNotOpenATerminal() {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(attachedItemId = "task-2", startupCommand = "claude --session-id theirs"))

        AgentRuntime.entries.forEach { runtime ->
            assertNull(startResumeTerminal(sessions, "task-1", "/work/alpha", runtime, "Resume"))
        }

        assertEquals(1, sessions.listSessions().size)
    }

    @Test
    fun resumingNeverTouchesAnAgentThatIsStillRunning() {
        val sessions = InMemoryTerminalSessionManager()
        val running = sessions.createSession(
            TerminalSessionSpec(title = "Claude", attachedItemId = "task-1", startupCommand = "claude 'fix it'"),
        )
        sessions.createSession(TerminalSessionSpec(attachedItemId = "task-1", startupCommand = "codex --no-daemon resume x9"))

        startResumeTerminal(sessions, "task-1", "/work/alpha", AgentRuntime.Codex, "Codex · resumed")

        assertEquals(3, sessions.listSessions().size)
        assertEquals("claude 'fix it'", sessions.listSessions().first { it.id == running.id }.spec.startupCommand)
    }

    @Test
    fun codexResumeRemainsIdentifiableWithTerminalStartupOptions() {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(attachedItemId = "task-1", startupCommand = "codex --no-daemon resume x9"))
        startResumeTerminal(sessions, "task-1", "/work/alpha", AgentRuntime.Codex, "Codex · resumed")

        val spec = sessions.listSessions().last().spec
        assertEquals("codex --no-daemon resume x9", spec.startupCommand)
        assertEquals(AgentRuntime.Codex, launchRuntimeOf(spec))
    }

    @Test
    fun anUnknownNewerConversationDoesNotResumeAnOlderOne() {
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(attachedItemId = "task-1", startupCommand = "codex --no-daemon resume old"))
        sessions.createSession(TerminalSessionSpec(attachedItemId = "task-1", startupCommand = "codex --no-daemon 'new task direction'"))

        assertNull(startResumeTerminal(sessions, "task-1", "/work/alpha", AgentRuntime.Codex, "Resume"))
        assertEquals(2, sessions.listSessions().size)
    }

    @Test
    fun aPlainTerminalHasNothingToResume() {
        val sessions = InMemoryTerminalSessionManager()

        assertNull(startResumeTerminal(sessions, "task-1", "/work/alpha", AgentRuntime.Terminal, "Terminal"))
        assertEquals(0, sessions.listSessions().size)
    }
}
