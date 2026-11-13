package app.thdev.moru.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

/** The quit question: it names how many terminals end, Quit reports the keep box, Cancel keeps Moru open. */
@OptIn(ExperimentalTestApi::class)
class QuitConfirmInteractionTest {
    @Test
    fun quittingEndsTheTerminalsUnlessTheKeepBoxIsTicked() = runDesktopComposeUiTest {
        val quits = mutableListOf<Boolean>()
        var cancels = 0
        setContent {
            MoruTheme { QuitConfirmContent(runningTerminals = 3, onQuit = { quits += it }, onCancel = { cancels++ }) }
        }
        onNodeWithText("3 terminals are running", substring = true).assertExists()
        onNodeWithText("Quit").performClick()
        onNodeWithContentDescription("Keep the terminals running this time").performClick()
        onNodeWithText("Quit").performClick()
        onNodeWithText("Cancel").performClick()
        runOnIdle {
            assertEquals(listOf(false, true), quits)
            assertEquals(1, cancels)
        }
    }

    @Test
    fun theQuestionIsAskedInKorean() = runDesktopComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Korean) {
                MoruTheme { QuitConfirmContent(runningTerminals = 2, onQuit = {}, onCancel = {}) }
            }
        }
        onNodeWithText("Moru를 종료할까요?").assertExists()
        onNodeWithText("터미널 2개가 실행 중입니다", substring = true).assertExists()
        onNodeWithContentDescription("이번에는 터미널 유지").assertExists()
    }
}
