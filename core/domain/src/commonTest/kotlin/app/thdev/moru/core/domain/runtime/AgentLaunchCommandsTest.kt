package app.thdev.moru.core.domain.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AgentLaunchCommandsTest {
    @Test fun retainsOrderedRuntimeChoicesAndExistingStartupRules() {
        assertEquals(listOf("claude", "codex", "agy", "terminal"), PortableAgentRuntime.entries.map { it.id })
        assertEquals("claude --session-id c1", AgentLaunchCommands.command(PortableAgentRuntime.Claude, "", "c1"))
        assertEquals("codex --no-daemon", AgentLaunchCommands.command(PortableAgentRuntime.Codex, "", "c1"))
        assertEquals("agy -i 'don'\\''t expand ${'$'}HOME or `id`\nkeep line'",
            AgentLaunchCommands.command(PortableAgentRuntime.Agy, "don't expand ${'$'}HOME or `id`\nkeep line", "c1"))
        assertEquals(null, AgentLaunchCommands.command(PortableAgentRuntime.Terminal, "ignored", "c1"))
    }
    @Test fun unsafeConversationIdentityCannotBecomeShellSyntax() {
        for (id in listOf("", "x;id", "x\nexit", "${'$'}(id)", "x y", "x'")) {
            assertFailsWith<IllegalArgumentException> { AgentLaunchCommands.command(PortableAgentRuntime.Claude, "", id) }
        }
    }
}
