package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsUpdateInteractionTest {
    @Test
    fun checkingDisablesDuplicateRequestsAndAReadyUpdateOffersRestart() = runDesktopComposeUiTest(width = 600, height = 350) {
        var state by mutableStateOf(SettingsUpdateState("26.41.3", action = SettingsUpdateAction.Check))
        var checks = 0
        var restarts = 0
        setContent {
            MoruTheme {
                Column(Modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page).padding(12.dp)) {
                    SettingsUpdateSection(state, onCheck = { checks++ }, onRestart = { restarts++ })
                }
            }
        }
        onNodeWithText("Current version: 26.41.3").assertExists()
        onNodeWithTag("settings-update-action").performClick()
        runOnIdle {
            assertEquals(1, checks)
            state = state.copy(message = "Checking for updates…", action = SettingsUpdateAction.Busy)
        }
        onNodeWithTag("settings-update-action").assertIsNotEnabled()
        onNodeWithTag("settings-update-status").assertTextContains("Checking for updates…")
        runOnIdle { state = state.copy(message = "Downloading Moru 26.42.1… 50%") }
        onNodeWithTag("settings-update-status").assertTextContains("Downloading Moru 26.42.1… 50%")
        captureSettings("settings-updates-downloading", "settings-updates")
        runOnIdle { state = state.copy(message = "Moru 26.42.1 is ready.", action = SettingsUpdateAction.Restart) }
        onNodeWithText("Restart to Update").performClick()
        runOnIdle { assertEquals(1, restarts) }
        captureSettings("settings-updates-ready", "settings-updates")
    }

    @Test
    fun aFailedCheckRemainsReadableAndRetryableInACompactLightPanel() = runDesktopComposeUiTest(width = 360, height = 400) {
        var checks = 0
        setContent {
            MoruTheme(darkTheme = false) {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Korean) {
                    Column(Modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page).padding(12.dp)) {
                        SettingsUpdateSection(
                            SettingsUpdateState("26.41.3", "Could not update Moru: The release server is unavailable. Check your connection and try again.",
                                SettingsUpdateAction.Check, failed = true),
                            onCheck = { checks++ }, onRestart = {},
                        )
                    }
                }
            }
        }
        onNodeWithTag("settings-update-status").assertTextContains("The release server is unavailable.", substring = true)
        onNodeWithTag("settings-update-action").assertIsEnabled().performClick()
        runOnIdle { assertEquals(1, checks) }
        captureSettings("settings-updates-failed-compact-light", "settings-updates")
    }
}
