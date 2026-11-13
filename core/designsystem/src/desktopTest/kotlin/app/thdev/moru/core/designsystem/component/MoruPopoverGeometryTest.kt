package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MoruPopoverGeometryTest {
    private val position = MoruPopoverPositionProvider()

    @Test
    fun nativeDrawingInsetsFitAtBothOppositeCorners() {
        val native = MoruPopoverPositionProvider(drawingInset = 2)
        val window = IntSize(320, 240)
        val popup = IntSize(180, 80)
        assertEquals(IntOffset(2, 2), native.calculatePosition(IntRect(0, 0, 0, 0), window, LayoutDirection.Ltr, popup))
        assertEquals(IntOffset(138, 158), native.calculatePosition(IntRect(320, 240, 320, 240), window, LayoutDirection.Ltr, popup))
    }

    @Test
    fun anEndAlignedPopupOpensTowardsItsPaneFromAnAnchorAtThePanesEnd() {
        // A sliver at the right end of a left pane, with room on both sides: the popup stays over that pane.
        val window = IntSize(1200, 600)
        val popup = IntSize(220, 300)
        val anchor = IntRect(590, 40, 591, 66)
        assertEquals(IntOffset(371, 66), MoruPopoverPositionProvider(alignEnd = true).calculatePosition(anchor, window, LayoutDirection.Ltr, popup))
        assertEquals(IntOffset(590, 66), position.calculatePosition(anchor, window, LayoutDirection.Ltr, popup))
        // Without room on its own side it still fits inside the window.
        val nearStart = IntRect(100, 40, 101, 66)
        assertEquals(IntOffset(100, 66), MoruPopoverPositionProvider(alignEnd = true).calculatePosition(nearStart, window, LayoutDirection.Ltr, popup))
    }

    @Test
    fun prefersBelowAndUsesAnchorEndAlignmentAtTheRightEdge() {
        val window = IntSize(320, 240)
        val popup = IntSize(180, 80)
        assertEquals(IntOffset(20, 40), position.calculatePosition(IntRect(20, 10, 50, 40), window, LayoutDirection.Ltr, popup))
        assertEquals(IntOffset(128, 40), position.calculatePosition(IntRect(280, 10, 308, 40), window, LayoutDirection.Ltr, popup))
    }

    @Test
    fun bottomAndOutOfWindowAnchorsStillKeepThePanelWithinTheOwner() {
        val window = IntSize(200, 120)
        val popup = IntSize(180, 80)
        assertEquals(IntOffset(20, 28), position.calculatePosition(IntRect(196, 108, 200, 112), window, LayoutDirection.Ltr, popup))
        assertEquals(IntOffset(0, 0), position.calculatePosition(IntRect(-40, -30, -20, -10), window, LayoutDirection.Ltr, popup))
        assertEquals(IntOffset(20, 40), position.calculatePosition(IntRect(250, 150, 260, 160), window, LayoutDirection.Ltr, popup))
    }

    @Test
    fun rightToLeftAnchorsPreferTheirLogicalStartAndFlipAtTheLeftEdge() {
        val window = IntSize(320, 240)
        val popup = IntSize(180, 80)
        assertEquals(IntOffset(120, 40), position.calculatePosition(IntRect(270, 10, 300, 40), window, LayoutDirection.Rtl, popup))
        assertEquals(IntOffset(12, 40), position.calculatePosition(IntRect(12, 10, 42, 40), window, LayoutDirection.Rtl, popup))
    }

    @Test
    fun aOneItemDropdownHasOnlyItsOwnCompactInsets() = runDesktopComposeUiTest(width = 320, height = 240) {
        setContent {
            MoruTheme {
                Box { MoruDropdownMenu(true, {}, listOf(MoruMenuItem("Settings", onClick = {})), Modifier.testTag("compact-popup")) }
            }
        }
        val popup = onNodeWithTag("compact-popup").fetchSemanticsNode().boundsInRoot
        assertEquals(180f, popup.width, 1f)
        assertTrue(popup.height <= 50f, "One 30dp action plus 6dp insets must not acquire Material menu padding")
    }

    @Test
    fun aLongDropdownScrollsToItsLastActionAndClosesAfterSelection() = runDesktopComposeUiTest(width = 480, height = 480) {
        var expanded by mutableStateOf(true)
        var selection = 0
        setContent {
            MoruTheme {
                Box {
                    MoruDropdownMenu(expanded, { expanded = false }, (1..30).map { index ->
                        MoruMenuItem("Action $index", onClick = { selection = index })
                    }, Modifier.testTag("long-popup"))
                }
            }
        }
        assertTrue(onNodeWithTag("long-popup").fetchSemanticsNode().boundsInRoot.height <= 360f)
        onNodeWithText("Action 30").performScrollTo().performClick()
        onNodeWithText("Action 1").assertDoesNotExist()
        runOnIdle { assertEquals(30, selection) }
    }
}
