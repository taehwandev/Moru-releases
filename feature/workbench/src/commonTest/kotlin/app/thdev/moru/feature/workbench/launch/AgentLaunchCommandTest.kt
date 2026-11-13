package app.thdev.moru.feature.workbench.launch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AgentLaunchCommandTest {
    @Test
    fun passesThePromptAsOneQuotedArgument() {
        assertEquals("claude --session-id c1 'fix the login bug'", agentLaunchCommand(AgentRuntime.Claude, "fix the login bug", "c1"))
        assertEquals("codex --no-daemon 'fix the login bug'", agentLaunchCommand(AgentRuntime.Codex, "fix the login bug", "c1"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("claude --session-id c1 'hello'", agentLaunchCommand(AgentRuntime.Claude, "  \n hello \n", "c1"))
    }

    @Test
    fun escapesAnApostropheSoTheShellKeepsItInsideTheQuotes() {
        assertEquals("claude --session-id c1 'don'\\''t stop'", agentLaunchCommand(AgentRuntime.Claude, "don't stop", "c1"))
    }

    @Test
    fun aBlankPromptStartsTheBareProgram() {
        assertEquals("claude --session-id c1", agentLaunchCommand(AgentRuntime.Claude, "   ", "c1"))
    }

    @Test
    fun everyClaudeTerminalStartsItsOwnConversation() {
        val first = agentLaunchCommand(AgentRuntime.Claude, "fix it")
        val second = agentLaunchCommand(AgentRuntime.Claude, "fix it")

        assertNotEquals(AgentConversation.idIn(first), AgentConversation.idIn(second))
    }

    @Test
    fun codexStartsWithoutTheSharedServerEvenWhenThePromptIsBlank() {
        assertEquals("codex --no-daemon", agentLaunchCommand(AgentRuntime.Codex, "   "))
        assertEquals("codex", AgentRuntime.Codex.program)
    }

    @Test
    fun agyIncludesInteractivePrompt() {
        assertEquals("agy -i 'fix the login bug'", agentLaunchCommand(AgentRuntime.Agy, "fix the login bug"))
        assertEquals("agy", agentLaunchCommand(AgentRuntime.Agy, "   "))
    }

    @Test
    fun aPlainTerminalTypesNothing() {
        assertEquals(null, agentLaunchCommand(AgentRuntime.Terminal, ""))
        assertEquals(null, agentLaunchCommand(AgentRuntime.Terminal, "fix the login bug"))
    }
}
