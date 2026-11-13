package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class MoruShareSplitTest {
    @Test
    fun aRowSeamDragsSidewaysAndKeepsTheMinimum() = runDesktopComposeUiTest(width = 1000, height = 400) {
        var shares by mutableStateOf(listOf(0.5f, 0.5f))
        setContent {
            MoruTheme {
                MoruShareSplit(
                    vertical = false,
                    shares = shares,
                    onSeamMove = { _, share -> shares = listOf(share, 1f - share) },
                    onSeamReset = { shares = listOf(0.5f, 0.5f) },
                    dividerLabel = "Resize", resetLabel = "Reset", minSize = 200.dp,
                ) { i, modifier -> Box(modifier.testTag("child$i")) }
            }
        }
        assertEquals(500f, onNodeWithTag("child0").fetchSemanticsNode().boundsInRoot.width, 1f)
        onNodeWithContentDescription("Resize").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(200f, 0f), delayMillis = 100)
            release()
        }
        runOnIdle { assertTrue(shares[0] > 0.6f, "Drag must widen the first child: $shares") }
        val seam = onNodeWithContentDescription("Resize")
        seam.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        seam.performKeyInput { repeat(40) { pressKey(Key.DirectionRight) } }
        runOnIdle { assertEquals(0.8f, shares[0], 0.001f) }
        seam.performKeyInput { pressKey(Key.Enter) }
        runOnIdle { assertEquals(listOf(0.5f, 0.5f), shares) }
    }

    @Test
    fun aColumnSeamAnswersTheUpAndDownArrows() = runDesktopComposeUiTest(width = 400, height = 1000) {
        var shares by mutableStateOf(listOf(0.5f, 0.5f))
        setContent {
            MoruTheme {
                MoruShareSplit(
                    vertical = true,
                    shares = shares,
                    onSeamMove = { _, share -> shares = listOf(share, 1f - share) },
                    onSeamReset = {},
                    dividerLabel = "Resize", resetLabel = "Reset", minSize = 100.dp,
                ) { i, modifier -> Box(modifier.testTag("child$i")) }
            }
        }
        assertEquals(500f, onNodeWithTag("child1").fetchSemanticsNode().boundsInRoot.height, 1f)
        val seam = onNodeWithContentDescription("Resize")
        seam.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        seam.performKeyInput { pressKey(Key.DirectionUp) }
        runOnIdle { assertEquals(0.476f, shares[0], 0.001f) }
        seam.performKeyInput { pressKey(Key.DirectionLeft) }
        runOnIdle { assertEquals(0.476f, shares[0], 0.001f) }
        assertEquals(524f, onNodeWithTag("child1").fetchSemanticsNode().boundsInRoot.height, 1f)
    }

    @Test
    fun aChildThatWouldBeTooSmallMakesEveryChildEvenUntilThereIsRoom() = runDesktopComposeUiTest(width = 1000, height = 400) {
        var shares by mutableStateOf(listOf(0.9f, 0.1f))
        setContent {
            MoruTheme {
                MoruShareSplit(
                    vertical = false,
                    shares = shares,
                    onSeamMove = { _, share -> shares = listOf(share, 1f - share) },
                    onSeamReset = {},
                    dividerLabel = "Resize", resetLabel = "Reset", minSize = 200.dp,
                ) { i, modifier -> Box(modifier.testTag("child$i")) }
            }
        }
        // A tenth of 1000 is below the 200 minimum: both get half, and the shares stay as they were.
        assertEquals(500f, onNodeWithTag("child1").fetchSemanticsNode().boundsInRoot.width, 1f)
        runOnIdle { assertEquals(listOf(0.9f, 0.1f), shares) }
        // The even layout only stands in for the shares, so dragging or stepping its seam leaves them alone.
        onNodeWithContentDescription("Resize").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(100f, 0f), delayMillis = 100)
            release()
        }
        val seam = onNodeWithContentDescription("Resize")
        seam.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        seam.performKeyInput { pressKey(Key.DirectionRight) }
        runOnIdle { assertEquals(listOf(0.9f, 0.1f), shares) }
        runOnIdle { shares = listOf(0.7f, 0.3f) }
        assertEquals(300f, onNodeWithTag("child1").fetchSemanticsNode().boundsInRoot.width, 1f)
    }
}
