package app.thdev.moru.feature.workbench.restore

import app.thdev.moru.core.model.AgentConversationRef
import app.thdev.moru.core.model.SavedTerminal
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.workbench.launch.AgentConversation
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClosedTerminalsTest {
    private val sessions = InMemoryTerminalSessionManager()

    private fun session(spec: TerminalSessionSpec): TerminalSessionSnapshot = sessions.createSession(spec)

    @Test
    fun anAgentWithAKnownConversationComesBackToThatConversation() {
        val claude = session(TerminalSessionSpec(title = "Claude", workingDirectory = "/p", attachedItemId = "t1", startupCommand = "claude --session-id abc 'fix it'"))

        val saved = ClosedTerminals.add(emptyList(), claude).single()

        assertEquals(SavedTerminal(title = "Claude", workingDirectory = "/p", taskId = "t1", startupCommand = "claude --resume abc"), saved)
    }

    @Test
    fun aShellOrAnAgentWithoutAKnownConversationComesBackAsAShell() {
        val shell = session(TerminalSessionSpec(title = "zsh", workingDirectory = "/p", attachedItemId = "t1"))
        val codex = session(TerminalSessionSpec(title = "Codex", workingDirectory = "/p", attachedItemId = "t1", startupCommand = "codex 'fix it'"))

        val closed = ClosedTerminals.add(ClosedTerminals.add(emptyList(), shell), codex)

        assertEquals(listOf(null, null), closed.map { it.startupCommand })
        assertEquals(listOf("zsh", "Codex"), closed.map { it.title })
    }

    @Test
    fun anAgentThatHadAlreadyEndedComesBackAsAShell() {
        val claude = session(TerminalSessionSpec(attachedItemId = "t1", startupCommand = "claude --session-id abc"))
        sessions.reportExit(claude.id, 0)

        assertNull(ClosedTerminals.add(emptyList(), sessions.listSessions().single()).single().startupCommand)
    }

    @Test
    fun aLabComesBackUnderItsKeySoItShowsTheSameConversation() {
        val lab = session(TerminalSessionSpec(title = "Claude Lab", kind = TerminalSessionKind.LAB, workingDirectory = "/p",
            attachedItemId = "t1", startupCommand = "claude", hostKey = "lab-1"))

        val saved = ClosedTerminals.add(emptyList(), lab).single()

        assertEquals(SavedTerminal(title = "Claude Lab", workingDirectory = "/p", taskId = "t1", startupCommand = "claude", hostKey = "lab-1", lab = true), saved)
        assertEquals(setOf("lab-1"), ClosedTerminals.labKeys(listOf(saved, SavedTerminal(taskId = "t1"))))
    }

    @Test
    fun aPreviewIsNotKept() {
        val preview = session(TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW, attachedItemId = "t1"))

        assertEquals(emptyList(), ClosedTerminals.add(emptyList(), preview))
    }

    @Test
    fun theConversationTheAgentHeldWinsEvenWhenTheStartupCommandNamesNone() {
        val typed = session(TerminalSessionSpec(title = "Terminal", workingDirectory = "/p", attachedItemId = "t1"))
        val codex = session(TerminalSessionSpec(title = "Codex", workingDirectory = "/p", attachedItemId = "t1", startupCommand = "codex"))
        val claude = session(TerminalSessionSpec(title = "Claude", attachedItemId = "t1", startupCommand = "claude --session-id old"))

        val closed = listOf(
            typed to AgentConversationRef("claude", "c1"),
            codex to AgentConversationRef("codex", "x2"),
            claude to AgentConversationRef("claude", "new"),
        ).fold(emptyList<SavedTerminal>()) { list, (terminal, running) -> ClosedTerminals.add(list, terminal, running) }

        assertEquals("claude --resume c1", closed[0].startupCommand)
        assertEquals(AgentConversation.resumeCommand(AgentRuntime.Codex, "x2"), closed[1].startupCommand)
        assertTrue(closed[1].startupCommand!!.contains("resume x2"))
        assertEquals("claude --resume new", closed[2].startupCommand)
    }

    @Test
    fun anAgentWhoseConversationIsUnknownComesBackAsThatAgent() {
        val agy = session(TerminalSessionSpec(title = "AGY", attachedItemId = "t1"))

        assertEquals("agy", ClosedTerminals.add(emptyList(), agy, AgentConversationRef("agy", null)).single().startupCommand)
    }

    @Test
    fun anUnavailableReadingKeepsTheCheckpointOfTheSameAgentAndNeverBorrowsAnotherProvidersId() {
        val known = session(TerminalSessionSpec(startupCommand = "claude --resume checkpointed", hostKey = "host"))
        assertEquals("claude --resume checkpointed", ClosedTerminals.add(emptyList(), known, AgentConversationRef("claude", null)).single().startupCommand)
        assertEquals("agy", ClosedTerminals.add(emptyList(), known, AgentConversationRef("agy", null)).single().startupCommand)
        sessions.reportExit(known.id, null)
        assertEquals("claude --resume checkpointed", ClosedTerminals.add(emptyList(), sessions.listSessions().single()).single().startupCommand)
    }

    @Test
    fun onlyTheNewestTwentyAreKept() {
        val closed = (1..25).fold(emptyList<SavedTerminal>()) { list, n ->
            ClosedTerminals.add(list, session(TerminalSessionSpec(title = "t$n", attachedItemId = "t1")))
        }

        assertEquals((6..25).map { "t$it" }, closed.map { it.title })
    }

    @Test
    fun theNewestTerminalWhoseTaskCanOpenIsChosen() {
        val closed = listOf(SavedTerminal(taskId = "a"), SavedTerminal(taskId = "b"), SavedTerminal(taskId = "gone"))

        assertEquals(1, ClosedTerminals.newestOpenable(closed) { it != "gone" })
        assertNull(ClosedTerminals.newestOpenable(closed) { false })
    }
}
