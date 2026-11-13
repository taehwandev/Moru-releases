package app.thdev.moru.feature.workbench.launch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentConversationTest {
    @Test
    fun readsTheConversationEachCliWasGiven() {
        assertEquals("c1", AgentConversation.idIn("claude --session-id c1 'fix it'"))
        assertEquals("c1", AgentConversation.idIn("/usr/local/bin/claude --resume c1 || claude --session-id c1"))
        assertEquals("x9", AgentConversation.idIn("codex --no-daemon resume x9 || codex --no-daemon"))
        assertEquals("a7", AgentConversation.idIn("agy --conversation a7 || agy"))
    }

    @Test
    fun aPromptOrAPickerIsNeverReadAsAConversation() {
        assertNull(AgentConversation.idIn("claude 'please --resume abc'"))
        assertNull(AgentConversation.idIn("claude \"please --resume abc\""))
        assertNull(AgentConversation.idIn("claude --resume"))
        assertNull(AgentConversation.idIn("codex --no-daemon resume || codex --no-daemon"))
        assertNull(AgentConversation.idIn("claude --continue || claude"))
        assertNull(AgentConversation.idIn("npm run dev --resume x"))
        assertNull(AgentConversation.idIn(null))
    }

    @Test
    fun newIdsAreDistinct() {
        assertEquals(100, List(100) { AgentConversation.newId() }.toSet().size)
    }

    @Test
    fun aKnownConversationResumeNeverFallsBackToAFreshConversation() {
        assertEquals("claude --resume c1", AgentConversation.resumeCommand(AgentRuntime.Claude, "c1"))
        assertEquals("codex --no-daemon resume x9", AgentConversation.resumeCommand(AgentRuntime.Codex, "x9"))
        assertEquals("agy --conversation a7", AgentConversation.resumeCommand(AgentRuntime.Agy, "a7"))
        assertNull(AgentConversation.resumeCommand(AgentRuntime.Terminal, "s1"))
    }

    @Test
    fun anInvalidExplicitConversationIdNeverBecomesShellSyntaxOrAFreshConversation() {
        val invalidIds = listOf("", " ", "c1;touch", "c1$(touch file)", "c1\nclaude", "'c1'", "\"c1\"", "--last")
        listOf(AgentRuntime.Claude, AgentRuntime.Codex, AgentRuntime.Agy).forEach { runtime ->
            invalidIds.forEach { id -> assertNull(AgentConversation.resumeCommand(runtime, id)) }
        }
        listOf("claude --resume c1;touch", "codex --no-daemon resume x9$(touch file)", "agy --conversation 'a7'").forEach { command ->
            assertNull(AgentConversation.idIn(command))
            assertNull(AgentConversation.resumeCommandFor(command))
            assertNull(AgentConversation.knownResumeCommandFor(command))
        }
    }

    @Test
    fun safeOpaqueConversationIdsKeepTheirOriginalValue() {
        val id = "c1.A_B:2-3"
        assertEquals(id, AgentConversation.idIn("claude --session-id $id 'fix it'"))
        assertEquals("claude --resume $id", AgentConversation.resumeCommand(AgentRuntime.Claude, id))
    }

    @Test
    fun taskResumeNamesTheConversationWithoutAnyPickerOrFreshStartFallback() {
        assertEquals("claude --resume c1", AgentConversation.knownResumeCommandFor("claude --session-id c1 'fix it'"))
        assertEquals("codex --no-daemon resume x9", AgentConversation.knownResumeCommandFor("codex --no-daemon resume x9 || codex --no-daemon"))
        assertEquals("agy --conversation a7", AgentConversation.knownResumeCommandFor("agy --conversation a7 || agy"))
        listOf(null, "claude", "claude --resume", "codex --no-daemon", "agy --continue").forEach { command ->
            assertNull(AgentConversation.knownResumeCommandFor(command))
        }
    }
}
