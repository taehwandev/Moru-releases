package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class UpdateSnackbarTest {
    @Test
    fun floatingFeedbackDoesNotResizeTheWorkspace() = runDesktopComposeUiTest(width = 760, height = 500) {
        var shown by mutableStateOf(false)
        setContent {
            MoruTheme {
                MoruFrame(MoruDestination.Workbench, {}, topInset = 0.dp,
                    banner = { if (shown) UpdateBanner(UpdateStatus.Ready("26.42.1"), {}, {}) },
                ) { Box(Modifier.fillMaxSize().testTag("workspace")) }
            }
        }
        val before = onNodeWithTag("workspace").fetchSemanticsNode().boundsInRoot
        runOnIdle { shown = true }
        onNodeWithTag("update-snackbar").assertIsDisplayed()
        assertEquals(before, onNodeWithTag("workspace").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun latestFeedbackHasNoDismissActionAndDoesNotStealEditorFocus() = runDesktopComposeUiTest(width = 760, height = 500) {
        var shown by mutableStateOf(false)
        var draft by mutableStateOf("")
        val focus = FocusRequester()
        setContent {
            MoruTheme {
                Box(Modifier.size(760.dp, 500.dp)) {
                    BasicTextField(draft, { draft = it }, Modifier.testTag("draft").focusRequester(focus))
                    if (shown) UpdateBanner(UpdateStatus.UpToDate("26.42.1"), {}, {})
                    LaunchedEffect(Unit) { focus.requestFocus() }
                }
            }
        }
        onNodeWithTag("draft").performTextInput("before")
        runOnIdle { shown = true }
        onNodeWithText("Moru 26.42.1 is the latest version.").assertExists()
        onNodeWithText("Dismiss").assertDoesNotExist()
        onNodeWithText("Later").assertDoesNotExist()
        onNodeWithTag("draft").assertIsFocused().performTextInput(" after")
        runOnIdle { assertEquals("before after", draft) }
    }

    @Test
    fun readyUpdateActionsRemainDistinctAndDoNotRunOnRender() = runDesktopComposeUiTest {
        var restarts = 0
        var dismissals = 0
        setContent { MoruTheme { UpdateSnackbarContent(UpdateStatus.Ready("26.42.1"), { restarts++ }, { dismissals++ }) } }
        runOnIdle { assertEquals(0, restarts); assertEquals(0, dismissals) }
        onNodeWithText("Later").performClick()
        runOnIdle { assertEquals(1, dismissals); assertEquals(0, restarts) }
        onNodeWithText("Restart to Update").performClick()
        runOnIdle { assertEquals(1, restarts) }
    }

    @Test
    fun compactStatesRenderInLightAndDarkWithReadableFeedback() = runDesktopComposeUiTest(width = 360, height = 320) {
        var status by mutableStateOf<UpdateStatus>(UpdateStatus.UpToDate("26.42.1"))
        var dark by mutableStateOf(false)
        setContent { MoruTheme(darkTheme = dark) { UpdateSnackbarContent(status, {}, {}, width = 320.dp) } }
        val states = listOf(
            "latest" to UpdateStatus.UpToDate("26.42.1"),
            "ready" to UpdateStatus.Ready("26.42.1"),
            "downloading" to UpdateStatus.Downloading("26.42.1", 42),
            "failed" to UpdateStatus.Failed("The connection was interrupted. Try again in Settings."),
        )
        for ((name, next) in states) for (isDark in listOf(false, true)) {
            runOnIdle { status = next; dark = isDark }
            waitForIdle()
            val file = File("build/reports/update-snackbar/$name-${if (isDark) "dark" else "light"}.png").apply { parentFile.mkdirs() }
            ImageIO.write(onNodeWithTag("update-snackbar").captureToImage().toAwtImage(), "png", file)
        }
    }
}
