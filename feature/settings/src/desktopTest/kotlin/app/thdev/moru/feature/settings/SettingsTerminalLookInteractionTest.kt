package app.thdev.moru.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.model.terminal.TerminalColorPresets
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The terminal look is set in Settings without importing anything, and one reset gives it back to Moru. */
@OptIn(ExperimentalTestApi::class)
class SettingsTerminalLookInteractionTest {
    private val dracula = TerminalColorPresets.all.first { it.name == "Dracula" }.scheme

    @Test
    fun aSchemeAColorACursorAndSpacingAreSetHereAndResetToMoru() = runDesktopComposeUiTest(width = 1300, height = 1400) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme(colors = darkCkColors) { SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal") }
        }
        runOnIdle { assertNull(repository.stored.terminal.profile) }

        onNodeWithTag("settings-terminal-scheme-dark").performScrollTo().performClick()
        onNodeWithTag("settings-terminal-scheme-dark-option-Dracula").performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile?.dark == dracula }
        runOnIdle { assertNull(repository.stored.terminal.profile?.light) }

        onNodeWithTag("settings-terminal-color-Red").performScrollTo().performClick()
        onNodeWithTag("settings-terminal-color-hex").performTextReplacement("#123456")
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile?.dark?.get(TerminalColorSlot.Red) == "#123456" }
        runOnIdle {
            val dark = repository.stored.terminal.profile?.dark
            assertEquals(dracula.background, dark?.background)
            assertEquals(dracula.with(TerminalColorSlot.Red, "#123456"), dark)
        }
        onNodeWithTag("settings-terminal-scheme-dark").assertExists()
        onNodeWithText("Custom").assertExists()

        onNodeWithText("Bar").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile?.cursor == TerminalCursorStyle.Bar }
        runOnIdle { assertEquals(true, repository.stored.terminal.profile?.cursorBlink) }
        onNodeWithTag("settings-terminal-line-spacing").performScrollTo().performTextReplacement("120")
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile?.lineSpacing == 1.2f }
        captureSettings("settings-terminal-look-custom")

        onNodeWithTag("settings-terminal-look-reset").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile == null }
        onNodeWithText("Custom").assertDoesNotExist()
    }

    @Test
    fun clearingTheOnlyColorSetGivesTheSchemeBackToMoru() = runDesktopComposeUiTest(width = 1300, height = 1400) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme(colors = darkCkColors) { SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal") }
        }
        onNodeWithTag("settings-terminal-color-Background").performScrollTo().performClick()
        onNodeWithTag("settings-terminal-color-hex").performTextReplacement("101010")
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile?.dark?.background == "#101010" }
        runOnIdle { assertNull(repository.stored.terminal.profile?.dark?.foreground) }

        onNodeWithTag("settings-terminal-color-use-moru").performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile == null }
    }
}
