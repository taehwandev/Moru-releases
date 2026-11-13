package app.thdev.moru.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.appTypography
import app.thdev.moru.core.model.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsFontInteractionTest {
    @Test
    fun fontSizesAreIndependentAndResetRepairsAnInvalidDraft() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        var applied by mutableStateOf(AppSettings())
        setContent {
            MoruTheme(typography = appTypography(applied.appearance.uiFontFamily, applied.appearance.uiFontSize)) {
                SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance", onSettingsSaved = { applied = it })
            }
        }
        onNodeWithTag("settings-ui-font-size-increase").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.uiFontSize == 15 }
        runOnIdle { assertEquals(13, applied.appearance.terminalFontSize) }
        captureSettings("settings-ide-font-preview")
        openSettingsPage(SettingsPage.Terminal)
        onNodeWithTag("settings-font-size-increase").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.terminalFontSize == 14 }
        runOnIdle { assertEquals(15, applied.appearance.uiFontSize) }
        onNodeWithTag("settings-font-size").performTextReplacement("28")
        waitUntil(timeoutMillis = 10_000) { applied.appearance.terminalFontSize == 28 }
        onNodeWithTag("settings-font-size-increase").assertIsNotEnabled()
        onNodeWithTag("settings-font-size").performTextReplacement("10")
        waitUntil(timeoutMillis = 10_000) { applied.appearance.terminalFontSize == 10 }
        onNodeWithTag("settings-font-size-decrease").assertIsNotEnabled()
        onNodeWithTag("settings-font-size").performTextReplacement("")
        onNodeWithTag("settings-font-reset").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.terminalFontSize == 13 }
        onNodeWithTag("settings-font-size").assertTextContains("13")
        openSettingsPage(SettingsPage.Appearance)
        onNodeWithTag("settings-ui-font-size").performScrollTo().performTextReplacement("28")
        waitUntil(timeoutMillis = 10_000) { applied.appearance.uiFontSize == 28 }
        captureSettings("settings-ide-font-largest")
        onNodeWithTag("settings-ui-font-reset").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.uiFontSize == 14 }
    }

    @Test
    fun aFontSelectionAndResetLeaveTheOtherFamilyUntouched() = runDesktopComposeUiTest(width = 900, height = 800) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme(darkTheme = true) {
                SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance", fontFamilies = listOf("Menlo", "Helvetica"))
            }
        }
        onNodeWithTag("settings-ui-font-family").performScrollTo().performClick()
        onNodeWithTag("settings-ui-font-family-search").performTextInput("helv")
        onNodeWithTag("settings-ui-font-family-option-Helvetica").performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.uiFontFamily == "Helvetica" }
        openSettingsPage(SettingsPage.Terminal)
        onNodeWithTag("settings-font-family").performScrollTo().performClick()
        onNodeWithTag("settings-font-family-option-Menlo").performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.terminalFontFamily == "Menlo" }
        onNodeWithTag("settings-font-reset").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.terminalFontFamily == null }
        runOnIdle { assertEquals("Helvetica", repository.stored.appearance.uiFontFamily) }
        captureSettings("settings-terminal-font-narrow-dark")
    }
}
