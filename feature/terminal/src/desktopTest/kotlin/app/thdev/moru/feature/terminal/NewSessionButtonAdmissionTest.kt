package app.thdev.moru.feature.terminal

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NewSessionButtonAdmissionTest {
    @Test
    fun disabledButtonIgnoresPointerUntilAdmissionReopens() = runDesktopComposeUiTest(width = 240, height = 60) {
        val enabled = mutableStateOf(false)
        var starts = 0
        setContent { MoruTheme { NewSessionButton(enabled.value) { starts++ } } }
        val button = onNodeWithContentDescription("New session")
        button.assertIsNotEnabled().performTouchInput { click() }
        runOnIdle { assertEquals(0, starts); enabled.value = true }
        button.assertIsEnabled().performTouchInput { click() }
        runOnIdle { assertEquals(1, starts) }
    }
}
