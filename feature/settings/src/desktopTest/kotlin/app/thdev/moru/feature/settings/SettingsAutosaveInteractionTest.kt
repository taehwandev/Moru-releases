package app.thdev.moru.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsAutosaveInteractionTest {
    @Test
    fun themeAndRememberStateArePersistedWithoutASaveButton() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        var applied by mutableStateOf(AppSettings())
        setContent {
            MoruTheme(darkTheme = applied.appearance.theme == AppTheme.Dark) {
                SettingsScreen(EmptyRuntimeStatus, repository, onSettingsSaved = { applied = it })
            }
        }
        onNodeWithText("Save Settings").assertDoesNotExist()
        onNodeWithTag("settings-save-status").assertTextContains("Changes save automatically.")
        runOnIdle { assertEquals(0, repository.saveCount.get()) }
        openSettingsPage(SettingsPage.Appearance)
        onNodeWithText("Dark").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.theme == AppTheme.Dark }
        onNodeWithTag("settings-save-status").assertTextContains("All changes saved.")
        captureSettings("settings-autosave-dark")
        openSettingsPage(SettingsPage.General)
        onNodeWithTag("settings-remember-state").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { !repository.stored.general.rememberLastState }
        runOnIdle { assertEquals(false, applied.general.rememberLastState) }
    }

    @Test
    fun aFailedSaveShowsAnErrorAndRetryStoresTheDraft() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository().apply { failWrites = true }
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance") } }
        onNodeWithText("Dark").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.attemptCount.get() > 0 }
        awaitSettingsText("Could not save changes: Disk unavailable")
        onNodeWithTag("settings-save-status").assertTextContains("Could not save changes: Disk unavailable")
        captureSettings("settings-save-failed")
        repository.failWrites = false
        onNodeWithTag("settings-save-retry").performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.theme == AppTheme.Dark }
        awaitSettingsText("All changes saved.")
        onNodeWithTag("settings-save-retry").assertDoesNotExist()
    }

    @Test
    fun numbersCanBeClearedAndReplacedWithoutSavingInvalidOrClampedValues() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent { MoruTheme(darkTheme = false) { SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal") } }
        onNodeWithTag("settings-font-size").performScrollTo().performTextReplacement("")
        onNodeWithTag("settings-font-size").assertTextContains("")
        onNodeWithText("Enter a whole number from 10 to 28.").assertExists()
        onNodeWithTag("settings-font-size").performTextReplacement("99")
        onNodeWithTag("settings-save-status").assertTextContains("Correct the highlighted numbers to save those changes.")
        runOnIdle {
            assertEquals(13, repository.stored.appearance.terminalFontSize)
            assertEquals(0, repository.saveCount.get())
        }
        captureSettings("settings-invalid-font-size-light")
        onNodeWithTag("settings-font-size").performTextReplacement("18")
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.terminalFontSize == 18 }
        awaitSettingsText("All changes saved.")
        onNodeWithTag("settings-scrollback").performScrollTo().performTextReplacement("499")
        runOnIdle { assertEquals(5000, repository.stored.terminal.scrollbackLines) }
        onNodeWithTag("settings-scrollback").performTextReplacement("20000")
        waitUntil(timeoutMillis = 10_000) { repository.stored.terminal.scrollbackLines == 20000 }
    }

    @Test
    fun fontsCanBeSearchedAndSelectedWithoutTypingTheirNamesIntoSettings() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, repository, page = "Terminal", fontFamilies = listOf("SF Mono", "Menlo")) } }
        onNodeWithTag("settings-font-family").performScrollTo().performClick()
        onNodeWithTag("settings-font-family-search").performTextInput("sf")
        runOnIdle { assertEquals(0, repository.saveCount.get()) }
        onNodeWithTag("settings-font-family-option-Menlo").assertDoesNotExist()
        captureSettings("settings-font-search", "settings-font-family-menu")
        onNodeWithTag("settings-font-family-option-SF Mono").performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.terminalFontFamily == "SF Mono" }
        onNodeWithTag("settings-font-family").assertTextContains("SF Mono", substring = true)
        runOnIdle { assertEquals(null, repository.stored.appearance.uiFontFamily) }
    }
}
