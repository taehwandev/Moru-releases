package app.thdev.moru.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CliUpdateTest {
    @Test
    fun versionIsReadFromEachCliVersionOutput() {
        assertEquals("2.1.293", parseCliVersion("2.1.293 (Claude Code)"))
        assertEquals("0.161.0", parseCliVersion("codex-cli 0.161.0\n"))
        assertEquals("1.3.1", parseCliVersion("1.3.1"))
        assertEquals("0.162.0-alpha.2", parseCliVersion("codex-cli 0.162.0-alpha.2"))
        assertNull(parseCliVersion("command not found"))
    }

    @Test
    fun newerVersionComparesNumbersNotText() {
        assertTrue(isNewerCliVersion("2.1.300", "2.1.293"))
        assertTrue(isNewerCliVersion("0.162.0", "0.161.9"))
        assertTrue(isNewerCliVersion("1.2.0", "1.2.0-beta"))
        assertFalse(isNewerCliVersion("2.1.293", "2.1.293"))
        assertFalse(isNewerCliVersion("2.1.29", "2.1.293"))
        assertFalse(isNewerCliVersion("1.2.0-beta", "1.2.0"))
        assertFalse(isNewerCliVersion("garbage", "1.0.0"))
    }

    @Test
    fun updateFollowsHowTheCliWasInstalled() {
        assertEquals(
            listOf("brew", "upgrade", "--cask", "claude-code@latest"),
            cliUpdateMethodOf("/opt/homebrew/Caskroom/claude-code@latest/2.1.293/claude").arguments("claude"),
        )
        assertEquals(
            listOf("brew", "upgrade", "codex"),
            cliUpdateMethodOf("/opt/homebrew/Cellar/codex/0.161.0/bin/codex").arguments("codex"),
        )
        assertEquals(
            CliUpdateMethod.Npm("@openai/codex"),
            cliUpdateMethodOf("/Users/me/.nvm/versions/node/v22/lib/node_modules/@openai/codex/bin/codex.js"),
        )
        assertEquals(
            listOf("codex", "update"),
            cliUpdateMethodOf("/Users/me/.codex/packages/standalone/releases/0.161.0-aarch64-apple-darwin/bin/codex").arguments("codex"),
        )
        assertEquals(
            listOf("claude", "update"),
            cliUpdateMethodOf("/Users/me/.local/share/claude/versions/2.1.293").arguments("claude"),
        )
    }

    @Test
    fun anNpmInstallIsLeftToTheUserBecauseItsChannelIsUnknown() {
        assertNull(cliUpdateMethodOf("/usr/local/lib/node_modules/@anthropic-ai/claude-code/cli.js").arguments("claude"))
    }

    @Test
    fun agyIsLaunchedAsItsOwnProgram() {
        assertEquals(listOf("claude", "codex", "antigravity"), AGENT_CLI_PROVIDERS)
        assertEquals("agy", providerProgram("antigravity"))
    }
}
