package app.thdev.moru.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.effectiveScrollbackLines
import app.thdev.moru.core.model.effectiveTerminalFontFamily
import app.thdev.moru.core.model.effectiveTerminalFontSize
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.model.terminal.TerminalImportItem
import app.thdev.moru.core.model.terminal.TerminalProfile
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsTerminalImportInteractionTest {
    private val imported = TerminalProfileImport.Imported(
        profile = TerminalProfile(
            source = "iTerm2 · Default", dark = TerminalColorScheme("#eeeeee", "#101010"),
            fontFamily = "Menlo", fontSize = 15, scrollbackLines = 1000,
        ),
        applied = listOf(TerminalImportItem.Colors, TerminalImportItem.Font, TerminalImportItem.Scrollback),
        skipped = listOf(TerminalImportItem.Transparency),
    )

    @Test
    fun anImportSitsOnTopOfTheUsersSettingsAndRemovingItBringsThemBack() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme {
                SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal", importTerminalProfile = { imported })
            }
        }
        onNodeWithTag("settings-terminal-import").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile == imported.profile }
        runOnIdle {
            val stored = repository.stored
            assertEquals(null, stored.appearance.terminalFontFamily)
            assertEquals(13, stored.appearance.terminalFontSize)
            assertEquals(5000, stored.terminal.scrollbackLines)
            assertEquals("Menlo", stored.effectiveTerminalFontFamily)
            assertEquals(15, stored.effectiveTerminalFontSize)
            assertEquals(1000, stored.effectiveScrollbackLines)
        }
        onNodeWithTag("settings-scrollback").assertTextContains("1000")
        val report = onAllNodesWithTag("settings-terminal-import-report")
        report[1].assertTextContains("Applied: colors, font, scrollback")
        report[2].assertTextContains("Not supported: transparency")
        captureSettings("settings-terminal-iterm2-import")

        onNodeWithTag("settings-terminal-look-reset").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile == null }
        runOnIdle {
            assertEquals(null, repository.stored.effectiveTerminalFontFamily)
            assertEquals(13, repository.stored.effectiveTerminalFontSize)
            assertEquals(5000, repository.stored.effectiveScrollbackLines)
        }
        onNodeWithTag("settings-scrollback").assertTextContains("5000")
    }

    @Test
    fun aValueTheUserSetsAfterImportingIsTheirsAndOutlivesTheImport() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme {
                SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal", importTerminalProfile = { imported })
            }
        }
        onNodeWithTag("settings-terminal-import").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile != null }
        onNodeWithTag("settings-scrollback").performScrollTo().performTextReplacement("2000")
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.scrollbackLines == 2000 }
        runOnIdle {
            assertEquals(2000, repository.stored.effectiveScrollbackLines)
            assertEquals(null, repository.stored.terminal.profile?.scrollbackLines)
            assertEquals("Menlo", repository.stored.effectiveTerminalFontFamily)
        }

        onNodeWithTag("settings-terminal-look-reset").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile == null }
        runOnIdle {
            assertEquals(2000, repository.stored.effectiveScrollbackLines)
            assertEquals(13, repository.stored.effectiveTerminalFontSize)
        }
    }

    @Test
    fun optionSendsEscapeIsOnByDefaultAndAnImportLeavesTheUsersChoice() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme {
                SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal", importTerminalProfile = { imported })
            }
        }
        runOnIdle { assertEquals(true, repository.stored.terminal.optionSendsEscape) }
        onNodeWithTag("settings-terminal-option-escape").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { !repository.stored.terminal.optionSendsEscape }

        onNodeWithTag("settings-terminal-import").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.profile == imported.profile }
        runOnIdle { assertEquals(false, repository.stored.terminal.optionSendsEscape) }
        captureSettings("settings-terminal-option-escape-off")
    }

    @Test
    fun aMissingITerm2SaysSoAndChangesNothing() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme { SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal") }
        }
        onNodeWithTag("settings-terminal-import").performScrollTo().performClick()
        onNodeWithTag("settings-terminal-import-report").assertTextContains("No iTerm2 settings were found on this Mac.")
        runOnIdle { assertEquals(null, repository.stored.terminal.profile) }
    }
}
