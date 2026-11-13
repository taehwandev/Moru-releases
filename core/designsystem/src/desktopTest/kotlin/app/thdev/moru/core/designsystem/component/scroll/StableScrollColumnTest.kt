package app.thdev.moru.core.designsystem.component.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StableScrollColumnTest {
    @Test
    fun ordinaryAbsoluteScrollMovesTheReadingPositionWhenEarlierContentGrows() = runDesktopComposeUiTest(width = 260, height = 160) {
        var above by mutableStateOf(40.dp)
        val scroll = ScrollState(0)
        setContent {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Box(Modifier.height(above))
                repeat(10) { Box(Modifier.fillMaxWidth().height(40.dp).testTag("row:$it")) }
            }
        }
        onNodeWithTag("row:9").performScrollTo()
        val before = onNodeWithTag("row:9").getUnclippedBoundsInRoot().top
        runOnIdle { above = 240.dp }
        assertEquals(before + 200.dp, onNodeWithTag("row:9").getUnclippedBoundsInRoot().top)
    }

    @Test
    fun nestedAnchorsStayPutOnTheFirstFrameWhenContentGrowsAndShrinksAtTheBottom() = runDesktopComposeUiTest(width = 260, height = 160) {
        var above by mutableStateOf(40.dp)
        val scroll = ScrollState(0)
        setContent {
            StableScrollColumn(scroll, Modifier.fillMaxSize()) {
                Column {
                    Box(Modifier.height(above))
                    repeat(10) { index -> key(index) { AnchorRow(index) } }
                }
            }
        }
        onNodeWithTag("row:9").performScrollTo()
        val before = onNodeWithTag("row:9").getUnclippedBoundsInRoot().top
        mainClock.autoAdvance = false
        try {
            listOf(240.dp, 20.dp, 180.dp, 40.dp).forEach { height ->
                runOnIdle { above = height }
                mainClock.advanceTimeByFrame()
                waitForIdle()
                val after = onNodeWithTag("row:9").getUnclippedBoundsInRoot().top
                assertTrue(abs((after - before).value) <= 1f, "Reading position changed from $before to $after")
            }
        } finally {
            mainClock.autoAdvance = true
        }
    }

    @Test
    fun aRemovedVisibleAnchorFallsBackToTheNextSurvivingRow() = runDesktopComposeUiTest(width = 260, height = 160) {
        var rows by mutableStateOf((0..9).toList())
        val scroll = ScrollState(0)
        setContent {
            StableScrollColumn(scroll, Modifier.fillMaxSize()) {
                Column { rows.forEach { index -> key(index) { AnchorRow(index) } } }
            }
        }
        onNodeWithTag("row:6").performScrollTo()
        val before = onNodeWithTag("row:4").getUnclippedBoundsInRoot().top
        runOnIdle { rows = rows - 3 }
        assertEquals(before, onNodeWithTag("row:4").getUnclippedBoundsInRoot().top)
        // A later explicit reveal remains free to move away from the retained reading position.
        onNodeWithTag("row:0").performScrollTo()
        assertEquals(0.dp, onNodeWithTag("row:0").getUnclippedBoundsInRoot().top)
    }

    @Composable
    private fun AnchorRow(index: Int) {
        Box(Modifier.fillMaxWidth().height(40.dp).stableScrollAnchor(index).testTag("row:$index"))
    }
}
