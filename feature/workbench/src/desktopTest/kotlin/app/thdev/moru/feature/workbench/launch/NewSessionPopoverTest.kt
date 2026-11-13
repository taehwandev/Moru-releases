package app.thdev.moru.feature.workbench.launch

import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NewSessionPopoverTest {
    @Test
    fun displayedNumberKeysAndKeypadStartTheirRuntimeOnce() = runDesktopComposeUiTest {
        val starts = mutableListOf<AgentRuntime>()
        setContent {
            MoruTheme {
                Box { NewSessionPopover(expanded = true, errorMessage = null, onStart = { starts += it }, onDismiss = {}) }
            }
        }
        listOf("1", "2", "3", "4").forEach { onNodeWithText(it).assertExists() }
        listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4).forEach { key ->
            onNodeWithText("Claude").performKeyInput { pressKey(key) }
        }
        runOnIdle {
            val expected = listOf(AgentRuntime.Claude, AgentRuntime.Codex, AgentRuntime.Agy, AgentRuntime.Terminal)
            assertEquals(expected + expected, starts)
        }
    }

    @Test
    fun modifiedNumbersAndOtherKeysDoNotStartAnythingAndEscapeStillCancels() = runDesktopComposeUiTest {
        val starts = mutableListOf<AgentRuntime>()
        var dismissals = 0
        setContent {
            MoruTheme {
                Box { NewSessionPopover(expanded = true, errorMessage = null, onStart = { starts += it }, onDismiss = { dismissals++ }) }
            }
        }
        listOf(Key.CtrlLeft, Key.MetaLeft, Key.AltLeft, Key.ShiftLeft).forEach { modifier ->
            onNodeWithText("Claude").performKeyInput {
                keyDown(modifier)
                pressKey(Key.One)
                keyUp(modifier)
            }
        }
        onNodeWithText("Claude").performKeyInput { pressKey(Key.Nine); pressKey(Key.Escape) }
        runOnIdle {
            assertEquals(emptyList(), starts)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun aHeldNumberLaunchesOnlyOnReleaseAndAStartupErrorRemainsRetryable() = runDesktopComposeUiTest {
        val starts = mutableListOf<AgentRuntime>()
        setContent {
            var error by remember { mutableStateOf<String?>(null) }
            MoruTheme {
                Box {
                    NewSessionPopover(expanded = true, errorMessage = error, onStart = { starts += it; error = "Unable to start" }, onDismiss = {})
                }
            }
        }
        onNodeWithText("Codex").performKeyInput { keyDown(Key.Two) }
        runOnIdle { assertEquals(emptyList(), starts) }
        onNodeWithText("Codex").performKeyInput { keyUp(Key.Two) }
        onNodeWithText("Unable to start").assertExists()
        onNodeWithText("Codex").performKeyInput { pressKey(Key.Two) }
        runOnIdle { assertEquals(listOf(AgentRuntime.Codex, AgentRuntime.Codex), starts) }
    }

    @Test
    fun mouseChoiceStillStartsAndClosedMenuLeavesNumbersToTheFocusedInput() = runDesktopComposeUiTest {
        val starts = mutableListOf<AgentRuntime>()
        val inputKeys = mutableListOf<Key>()
        setContent {
            var open by remember { mutableStateOf(true) }
            var text by remember { mutableStateOf("") }
            MoruTheme {
                Box {
                    BasicTextField(
                        text,
                        onValueChange = { text = it },
                        modifier = Modifier.testTag("underlying-input").onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown) inputKeys += it.key
                            false
                        },
                    )
                    NewSessionPopover(expanded = open, errorMessage = null, onStart = { starts += it; open = false }, onDismiss = { open = false })
                }
            }
        }
        // The hint overlays the action row; pressing it must still hit the same action.
        onNodeWithText("4").performMouseInput { moveTo(center); press(); release() }
        onNodeWithText("Claude").assertDoesNotExist()
        onNodeWithTag("underlying-input").performClick().performKeyInput { pressKey(Key.One) }
        runOnIdle {
            assertEquals(listOf(AgentRuntime.Terminal), starts)
            assertEquals(listOf(Key.One), inputKeys)
        }
    }
}
