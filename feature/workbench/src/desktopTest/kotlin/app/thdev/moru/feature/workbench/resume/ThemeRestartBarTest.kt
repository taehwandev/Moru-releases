package app.thdev.moru.feature.workbench.resume

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toAwtImage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class ThemeRestartBarTest {
    private val session = TerminalSessionSnapshot(
        "codex-2", TerminalSessionSpec(title = "Fix colors", attachedItemId = "task", startupCommand = "codex --no-daemon resume conversation-2"),
        TerminalSessionStatus.READY,
    )

    @Test
    fun theNoticeNamesTheTerminalAndOnlyRestartsOnClick() = runDesktopComposeUiTest {
        var restarts = 0
        setContent {
            MoruTheme {
                ThemeRestartBar(session, SessionActivity.Waiting, false, false, { restarts++ }, Modifier.width(700.dp))
            }
        }
        onNodeWithText("Codex theme changed · Fix colors (codex-2)").assertExists()
        onNodeWithText("Restart to apply the theme and resume this conversation in the same terminal. Unsubmitted input may be lost.").assertExists()
        val capture = File("build/ui-captures/theme-restart-light.png")
        capture.parentFile.mkdirs()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", capture)
        runOnIdle { assertEquals(0, restarts) }
        onNodeWithText("Restart for theme").performClick()
        runOnIdle { assertEquals(1, restarts) }
    }

    @Test
    fun anUnknownConversationExplainsTheCodexPicker() = runDesktopComposeUiTest {
        setContent {
            MoruTheme {
                ThemeRestartBar(session.copy(spec = session.spec.copy(startupCommand = "codex --no-daemon")), SessionActivity.Waiting, false, false, {})
            }
        }
        onNodeWithText("Restart to apply the theme. Codex will open its conversation picker in this terminal. Unsubmitted input may be lost.").assertExists()
    }

    @Test
    fun busyAndOutstandingRequestsDisableTheActionAndFailureAllowsRetry() = runDesktopComposeUiTest {
        val activity = mutableStateOf(SessionActivity.Working)
        val inProgress = mutableStateOf(false)
        val failed = mutableStateOf(false)
        var restarts = 0
        setContent {
            MoruTheme {
                ThemeRestartBar(session, activity.value, inProgress.value, failed.value, { restarts++ }, Modifier.width(700.dp))
            }
        }
        onNodeWithText("Restart for theme").assertIsNotEnabled()
        runOnIdle { activity.value = SessionActivity.Asking }
        onNodeWithText("Restart for theme").assertIsNotEnabled()
        runOnIdle { activity.value = SessionActivity.Waiting; inProgress.value = true }
        onNodeWithText("Restart for theme").assertIsNotEnabled()
        onNodeWithText("Restarting Codex…").assertExists()
        runOnIdle { inProgress.value = false; failed.value = true }
        onNodeWithText("Codex could not restart safely. Try again when this session is idle.").assertExists()
        onNodeWithText("Restart for theme").assertIsEnabled().performClick()
        runOnIdle { assertEquals(1, restarts) }
    }
}
