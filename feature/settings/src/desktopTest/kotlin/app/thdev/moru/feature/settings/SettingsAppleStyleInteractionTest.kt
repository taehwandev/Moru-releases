package app.thdev.moru.feature.settings

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.AppearanceSettings
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsAppleStyleInteractionTest {
    @Test
    fun appleStyleSavesReloadsAndStaysIndependentOfThePalette() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        val repository = UiSettingsRepository().apply {
            stored = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Dark))
        }
        var applied by mutableStateOf(repository.stored)
        var generation by mutableStateOf(0)
        val material = AtomicReference<MoruMaterialStyle>()
        setContent {
            MoruTheme(
                colors = if (applied.appearance.theme == AppTheme.Light) lightCkColors else darkCkColors,
                materialStyle = if (applied.appearance.appleStyle) MoruMaterialStyle.Apple else MoruMaterialStyle.Standard,
            ) {
                val current = MoruTheme.materialStyle
                SideEffect { material.set(current) }
                key(generation) { SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance", onSettingsSaved = { applied = it }) }
            }
        }
        runOnIdle { assertFalse(repository.stored.appearance.appleStyle) }
        onNodeWithTag("settings-apple-style").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.appleStyle && material.get() == MoruMaterialStyle.Apple }
        runOnIdle { assertEquals(AppTheme.Dark, repository.stored.appearance.theme); generation++ }
        onNodeWithTag("settings-apple-style").performScrollTo().assertExists()
        onNodeWithText("Light").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.theme == AppTheme.Light }
        runOnIdle { assertTrue(repository.stored.appearance.appleStyle) }
        captureSettings("settings-apple-style-enabled")
        onNodeWithTag("settings-apple-style").performClick()
        waitUntil(timeoutMillis = 10_000) { !applied.appearance.appleStyle && material.get() == MoruMaterialStyle.Standard }
        runOnIdle { assertEquals(AppTheme.Light, repository.stored.appearance.theme) }
        captureSettings("settings-apple-style-disabled")
    }

    @Test
    fun failedAppleStyleSaveKeepsTheAppliedStyleUntilRetry() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        val repository = UiSettingsRepository().apply { failWrites = true }
        var applied by mutableStateOf(repository.stored)
        setContent { MoruTheme { SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance", onSettingsSaved = { applied = it }) } }
        onNodeWithTag("settings-apple-style").performScrollTo().performClick()
        awaitSettingsText("Could not save changes: Disk unavailable")
        runOnIdle {
            assertFalse(applied.appearance.appleStyle)
            assertFalse(repository.stored.appearance.appleStyle)
        }
        repository.failWrites = false
        onNodeWithTag("settings-save-retry").performClick()
        waitUntil(timeoutMillis = 10_000) { applied.appearance.appleStyle }
        runOnIdle { assertEquals(AppTheme.System, repository.stored.appearance.theme) }
    }
}
