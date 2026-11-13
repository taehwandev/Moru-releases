package app.thdev.moru.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalAgentCliCommandsTest {
    @Test
    fun localCommandsWithInvalidArgumentsAreRecognizedButCannotExecute() {
        for (separator in listOf(" ", "\n", "\t", "\r", "\u00a0")) {
            val text = "/login${separator}unexpected"
            assertEquals(LocalAgentCliAction.Login, LocalAgentCliCommands.parse(text))
            assertFalse(LocalAgentCliCommands.isExact(text, LocalAgentCliAction.Login))
        }
        assertEquals(LocalAgentCliAction.Clear, LocalAgentCliCommands.parse("  /CLEAR  "))
        assertTrue(LocalAgentCliCommands.isExact("  /CLEAR  ", LocalAgentCliAction.Clear))
        assertNull(LocalAgentCliCommands.parse("please /logout"))
        assertNull(LocalAgentCliCommands.parse("/usage-credits"))
        assertNull(LocalAgentCliCommands.parse("/login; arbitrary"))
    }

    @Test
    fun authenticationUsesTheLocalCliCredentialCommands() {
        assertEquals(LocalAgentCliLaunch("exec claude auth login"), LocalAgentCliCommands.launch("claude", LocalAgentCliAction.Login))
        assertEquals(LocalAgentCliLaunch("exec claude auth logout"), LocalAgentCliCommands.launch("claude", LocalAgentCliAction.Logout))
        assertEquals(LocalAgentCliLaunch("exec codex login"), LocalAgentCliCommands.launch("codex", LocalAgentCliAction.Login))
        assertEquals(LocalAgentCliLaunch("exec codex logout"), LocalAgentCliCommands.launch("codex", LocalAgentCliAction.Logout))
        assertEquals(LocalAgentCliLaunch("exec agy", "/login"), LocalAgentCliCommands.launch("agy", LocalAgentCliAction.Login))
        assertEquals(LocalAgentCliLaunch("exec agy", "/logout"), LocalAgentCliCommands.launch("agy", LocalAgentCliAction.Logout))
    }

    @Test
    fun nativeMenusNeverBecomeInitialModelPromptsOrSpendTickets() {
        for (provider in listOf("claude", "codex")) {
            for (action in listOf(LocalAgentCliAction.Status, LocalAgentCliAction.Usage)) {
                if (provider == "claude" && action == LocalAgentCliAction.Usage) continue
                val plan = LocalAgentCliCommands.launch(provider, action) ?: continue
                assertFalse(plan.startupCommand.contains("/${action.commandName}"))
                assertEquals("/${action.commandName}", plan.slashCommand)
                assertFalse(plan.startupCommand.contains("consume"))
            }
        }
        assertEquals("exec codex --no-daemon", LocalAgentCliCommands.launch("codex", LocalAgentCliAction.Usage)?.startupCommand)
    }

    @Test
    fun supportedNativeUsagePrintCommandsReturnResultsWithoutAnInteractiveComposer() {
        assertEquals(LocalAgentCliLaunch("exec claude --print /usage"), LocalAgentCliCommands.launch("claude", LocalAgentCliAction.Usage))
        for (provider in listOf("agy", "agy-real", "antigravity")) {
            assertEquals(LocalAgentCliLaunch("exec agy -p /usage"), LocalAgentCliCommands.launch(provider, LocalAgentCliAction.Usage))
        }
    }

    @Test
    fun clearDoesNotLaunchAnotherCliAndUnknownProgramsCannotBeExecuted() {
        for (provider in listOf("claude", "codex", "agy")) {
            assertNull(LocalAgentCliCommands.launch(provider, LocalAgentCliAction.Clear))
            assertTrue(LocalAgentCliAction.Clear in LocalAgentCliCommands.actions(provider))
        }
        assertNull(LocalAgentCliCommands.launch("codex; arbitrary", LocalAgentCliAction.Login))
        assertTrue(LocalAgentCliCommands.actions("missing").isEmpty())
        assertNull(LocalAgentCliCommands.launch("agy", LocalAgentCliAction.Status))
        assertFalse(LocalAgentCliAction.Status in LocalAgentCliCommands.actions("agy"))
    }
}
