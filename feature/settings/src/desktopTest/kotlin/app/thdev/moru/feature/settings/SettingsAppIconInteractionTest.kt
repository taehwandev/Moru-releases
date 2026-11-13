package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.AppIcon
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppearanceSettings
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsAppIconInteractionTest {
    @Test
    fun pickingAnIconSavesItAndAnUnknownOneShowsTheDefault() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        // A later build's choice this one does not know reads as the default.
        val repository = UiSettingsRepository().apply { stored = AppSettings(appearance = AppearanceSettings(appIcon = "rocket")) }
        var applied by mutableStateOf(repository.stored)
        setContent {
            MoruTheme {
                SettingsScreen(
                    EmptyRuntimeStatus, repository, page = "Appearance", onSettingsSaved = { applied = it },
                    appIconPreview = { _, modifier -> Box(modifier.background(Color.Gray)) },
                )
            }
        }
        onNodeWithText("App icon").performScrollTo().assertExists()
        onNodeWithTag("settings-app-icon-moru").assertIsSelected()

        onNodeWithTag("settings-app-icon-mo").performScrollTo().performClick()

        waitUntil(timeoutMillis = 10_000) { applied.appearance.appIcon == AppIcon.Mo.id }
        runOnIdle { assertEquals(AppIcon.Mo.id, repository.stored.appearance.appIcon) }
        onNodeWithTag("settings-app-icon-mo").assertIsSelected()
    }

    @Test
    fun withoutPicturesTheChoiceIsLeftOut() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, UiSettingsRepository(), page = "Appearance") } }
        onNodeWithTag("settings-apple-style").performScrollTo().assertExists()
        onNodeWithText("App icon").assertDoesNotExist()
    }
}
