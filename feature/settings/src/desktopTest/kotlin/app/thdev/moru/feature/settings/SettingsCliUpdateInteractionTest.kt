package app.thdev.moru.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.runtime.AgentCli
import app.thdev.moru.core.runtime.CliUpdateOutcome
import app.thdev.moru.core.runtime.RuntimeStatusRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsCliUpdateInteractionTest {
    /** Codex from Homebrew, which Moru can update, and Claude from npm, which it leaves to the user. */
    private class CliRuntime(var installed: String, private val next: String, private val failing: Boolean = false) :
        RuntimeStatusRepository {
        val updates = AtomicInteger()

        override fun installedAgentClis() = listOf(
            AgentCli("claude", "/usr/local/bin/claude"),
            AgentCli("codex", "/opt/homebrew/bin/codex"),
        )

        override fun installedVersion(provider: String) = when (provider) {
            "codex" -> installed
            "claude" -> "2.1.286"
            else -> null
        }
        override fun updateCommand(provider: String) = "brew upgrade --cask codex".takeIf { provider == "codex" }
        override fun runUpdate(provider: String): CliUpdateOutcome {
            updates.incrementAndGet()
            if (failing) return CliUpdateOutcome.Failed("brew upgrade --cask codex", "Permission denied")
            val from = installed
            installed = next
            return CliUpdateOutcome.Updated(from, next)
        }
    }

    @Test
    fun installedClisShowTheirVersionAndUpdateOnlyWhenAsked() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val runtime = CliRuntime(installed = "0.161.0", next = "0.162.0")
        setContent { MoruTheme { SettingsScreen(runtime, UiSettingsRepository(), page = "Updates", fontFamilies = emptyList(), cliUpdateDispatcher = Dispatchers.Unconfined) } }

        awaitSettingsText("Codex 0.161.0")
        onNodeWithText("Claude 2.1.286").assertExists()
        onNodeWithText("brew upgrade --cask codex").assertExists()
        onNodeWithText("Installed with npm, so Moru cannot tell its release channel. Update it with npm the way you installed it.")
            .assertExists()
        onNodeWithText("Ollama").assertDoesNotExist()
        assertEquals(0, runtime.updates.get(), "opening Settings never updates on its own")
        onNodeWithTag("settings-agent-clis").performScrollTo()
        captureSettings("settings-agent-clis")

        onNodeWithText("Update").performScrollTo().performClick()
        awaitSettingsText("Updated to 0.162.0. Idle sessions restart now; busy ones restart when their turn ends.")
        assertEquals(1, runtime.updates.get())
        onNodeWithText("Codex 0.162.0").assertExists()
        captureSettings("settings-agent-cli-updated")
    }

    @Test
    fun aFailedUpdateSaysWhatToRunByHand() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val runtime = CliRuntime(installed = "0.161.0", next = "0.162.0", failing = true)
        setContent { MoruTheme { SettingsScreen(runtime, UiSettingsRepository(), page = "Updates", fontFamilies = emptyList(), cliUpdateDispatcher = Dispatchers.Unconfined) } }

        awaitSettingsText("Codex 0.161.0")
        onNodeWithText("Update").performScrollTo().performClick()
        awaitSettingsText("Update failed: Permission denied Run it in a terminal: brew upgrade --cask codex")
    }

    @Test
    fun aMachineWithoutAgentClisSaysWhatToInstall() = runDesktopComposeUiTest(width = 1300, height = 900) {
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, UiSettingsRepository(), page = "Updates", fontFamilies = emptyList(), cliUpdateDispatcher = Dispatchers.Unconfined) } }

        awaitSettingsText("No agent CLI found")
        onNodeWithText("Install Claude Code, Codex or Antigravity (agy), then open Settings again.").assertExists()
    }
}
