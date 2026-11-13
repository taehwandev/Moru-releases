package app.thdev.moru.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.model.AppTheme
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsNavigationInteractionTest {
    @Test
    fun anAppOwnedEditorFinishesItsSaveAfterTheSettingsScreenCloses() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val releaseWrite = CountDownLatch(1)
        val repository = UiSettingsRepository().apply {
            beforeWrite = { check(releaseWrite.await(10, TimeUnit.SECONDS)) }
        }
        var showSettings by mutableStateOf(true)
        var appliedTheme by mutableStateOf(AppTheme.System)
        lateinit var editor: SettingsEditorState
        setContent {
            editor = rememberSettingsEditorState(repository) { appliedTheme = it.appearance.theme }
            MoruTheme {
                if (showSettings) SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance", editor = editor)
                else MoruText("Another destination")
            }
        }
        try {
            onNodeWithText("Dark").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10_000) { repository.attemptCount.get() == 1 }
            onNodeWithTag("settings-save-status").assertTextContains("Saving changes…")
            runOnIdle { showSettings = false }
            onNodeWithText("Another destination").assertExists()
            releaseWrite.countDown()
            waitUntil(timeoutMillis = 10_000) { appliedTheme == AppTheme.Dark }
            runOnIdle { showSettings = true }
            // The save already landed; coming back shows the neutral hint, not a stale confirmation.
            onNodeWithTag("settings-save-status").assertTextContains("Changes save automatically.")
            runOnIdle { assertEquals(1, repository.saveCount.get()) }
        } finally {
            releaseWrite.countDown()
        }
    }

    @Test
    fun automaticSaveFeedbackIsReadableInKoreanAndLightTheme() = runDesktopComposeUiTest(width = 1300, height = 900) {
        val repository = UiSettingsRepository()
        setContent {
            MoruTheme(darkTheme = false) {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Korean) {
                    SettingsScreen(EmptyRuntimeStatus, repository, page = "Appearance")
                }
            }
        }
        onNodeWithTag("settings-save-status").assertTextContains("변경 사항을 자동으로 저장합니다.")
        onNodeWithText("다크").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { repository.stored.appearance.theme == AppTheme.Dark }
        awaitSettingsText("모든 변경 사항을 저장했습니다.")
        captureSettings("settings-autosave-korean-light")
    }
}
