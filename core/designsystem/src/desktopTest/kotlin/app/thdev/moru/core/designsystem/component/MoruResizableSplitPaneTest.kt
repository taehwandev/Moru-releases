package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MoruResizableSplitPaneTest {
    @Test
    fun dragEnlargesRightPaneAndKeyboardResetsWithoutExceedingBounds() = runDesktopComposeUiTest(width = 1000, height = 400) {
        var width by mutableStateOf(400.dp)
        setContent {
            MoruTheme {
                MoruResizableSplitPane(width, { width = it }, { width = 400.dp }, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    left = { Box(it.testTag("left")) }, right = { Box(it.testTag("right")) })
            }
        }
        val divider = onNodeWithContentDescription("Resize panes")
        divider.performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(-160f, 0f), delayMillis = 100)
            release()
        }
        runOnIdle { assertTrue(width > 500.dp, "Drag must use the full displacement") }
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { repeat(25) { pressKey(Key.DirectionLeft) } }
        runOnIdle { assertEquals(692.dp, width) }
        divider.performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(400.dp, width) }
        divider.performKeyInput { repeat(25) { pressKey(Key.DirectionRight) } }
        runOnIdle { assertEquals(240.dp, width) }
    }

    @Test
    fun arrowKeysBeforeTheNextFrameAddUpInsteadOfRepeatingOneStep() = runDesktopComposeUiTest(width = 1000, height = 400) {
        val reported = mutableListOf<Float>()
        setContent {
            MoruTheme {
                // The caller does not apply the reported width, so no frame between the presses moves the pane.
                MoruResizableSplitPane(400.dp, { reported += it.value }, {}, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    left = { Box(it.testTag("left")) }, right = { Box(it.testTag("right")) })
            }
        }
        val divider = onNodeWithContentDescription("Resize panes")
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { repeat(3) { pressKey(Key.DirectionLeft) } }
        runOnIdle { assertEquals(listOf(424f, 448f, 472f), reported) }
    }

    @Test
    fun aShareSplitReportsTheDraggedWidthAsAShareOfTheSplit() = runDesktopComposeUiTest(width = 1000, height = 400) {
        var share by mutableStateOf(0.5f)
        setContent {
            MoruTheme {
                MoruResizableShareSplitPane(share, { share = it }, { share = 0.5f }, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    left = { Box(it.testTag("left")) }, right = { Box(it.testTag("right")) })
            }
        }
        assertEquals(500f, onNodeWithTag("right").fetchSemanticsNode().boundsInRoot.width, 1f)
        val divider = onNodeWithContentDescription("Resize panes")
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { pressKey(Key.DirectionLeft) }
        runOnIdle { assertEquals(0.524f, share, 0.001f) }
        divider.performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(0.5f, share) }
    }

    @Test
    fun anOverlayDividerKeepsPanesAdjacentAndSupportsDragKeyboardAndReset() = runDesktopComposeUiTest(width = 1000, height = 400) {
        var share by mutableStateOf(0.5f)
        setContent {
            MoruTheme {
                MoruResizableShareSplitPane(
                    share, { share = it }, { share = 0.5f }, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    dividerOverlayModifier = Modifier.height(30.dp),
                    left = { Box(it.testTag("left")) }, right = { Box(it.testTag("right")) },
                )
            }
        }
        val left = onNodeWithTag("left").fetchSemanticsNode().boundsInRoot
        val right = onNodeWithTag("right").fetchSemanticsNode().boundsInRoot
        val divider = onNodeWithContentDescription("Resize panes")
        val target = divider.fetchSemanticsNode().boundsInRoot
        assertEquals(left.right, right.left, 1f, "An overlaid divider must not reserve an empty lane")
        assertEquals(1000f, left.width + right.width, 1f)
        assertEquals(left.right, target.center.x, 1f, "The target must center on the seam")
        assertEquals(8f, target.width, 1f)
        assertEquals(30f, target.height, 1f)
        divider.performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(-160f, 0f), delayMillis = 100)
            release()
        }
        runOnIdle { assertTrue(share > 0.65f, "The overlay must receive the full drag displacement") }
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { repeat(25) { pressKey(Key.DirectionLeft) } }
        runOnIdle { assertEquals(0.7f, share, 0.001f) }
        divider.performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(0.5f, share, 0.001f) }
        divider.performKeyInput { repeat(25) { pressKey(Key.DirectionRight) } }
        runOnIdle { assertEquals(0.24f, share, 0.001f) }
        divider.performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
        runOnIdle { assertEquals(0.5f, share, 0.001f) }
    }

    @Test
    fun aCompactWindowFitsBothPanesEvenWhenBothMinimaCannotFit() = runDesktopComposeUiTest(width = 360, height = 300) {
        setContent {
            MoruTheme {
                MoruResizableSplitPane(900.dp, {}, {}, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    left = { Box(it.testTag("left")) }, right = { Box(it.testTag("right")) })
            }
        }
        val left = onNodeWithTag("left").fetchSemanticsNode().boundsInRoot
        val right = onNodeWithTag("right").fetchSemanticsNode().boundsInRoot
        assertTrue(left.width > 0f && right.width > 0f)
        assertTrue(right.right <= 360f)
        assertEquals(352f, left.width + right.width, 1f)
    }
}
