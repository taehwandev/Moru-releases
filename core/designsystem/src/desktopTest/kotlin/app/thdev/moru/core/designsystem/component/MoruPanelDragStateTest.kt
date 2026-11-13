package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class MoruPanelDragStateTest {
    @Test
    fun removingTheTargetDuringADragClearsThePreviewAndDoesNotDropOnTheClosedPanel() = runDesktopComposeUiTest(width = 800, height = 400) {
        lateinit var state: MoruPanelDragState
        val showTarget = mutableStateOf(true)
        val moves = mutableListOf<Any>()
        setContent {
            MoruTheme {
                state = remember { MoruPanelDragState() }
                Row {
                    Column(Modifier.size(400.dp, 400.dp).then(state.dropTarget("a"))) {
                        Box(Modifier.fillMaxWidth().height(30.dp).testTag("handle-a")
                            .then(state.handle("a") { _, to, _ -> moves += to }))
                    }
                    if (showTarget.value) Box(Modifier.size(400.dp, 400.dp).then(state.dropTarget("b")))
                }
            }
        }
        onNodeWithTag("handle-a").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(200f, 100f), delayMillis = 50)
            moveTo(center + Offset(400f, 200f), delayMillis = 50)
        }
        runOnIdle { assertEquals("b", state.target); showTarget.value = false }
        runOnIdle { assertNull(state.target); assertNull(state.landing) }
        onNodeWithTag("handle-a").performMouseInput { release() }
        runOnIdle { assertEquals(emptyList(), moves) }
    }

    @Test
    fun aSwapOnlyHandlePreviewsTheWholeTargetEvenWhenDroppedNearItsHeader() = runDesktopComposeUiTest(width = 800, height = 400) {
        lateinit var state: MoruPanelDragState
        val moves = mutableListOf<MoruDropEdge>()
        setContent {
            MoruTheme {
                state = remember { MoruPanelDragState(allowEdges = false) }
                Row {
                    listOf("a", "b").forEach { key ->
                        Column(Modifier.size(400.dp, 400.dp).then(state.dropTarget(key))) {
                            Box(Modifier.fillMaxWidth().height(30.dp).testTag("handle-$key")
                                .then(state.handle(key) { _, _, edge -> moves += edge }))
                        }
                    }
                }
            }
        }
        onNodeWithTag("handle-a").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(200f, 0f), delayMillis = 50)
            moveTo(center + Offset(400f, 0f), delayMillis = 50)
        }
        runOnIdle {
            assertEquals("b", state.target)
            assertEquals(MoruDropEdge.Center, state.edge)
            assertEquals(androidx.compose.ui.geometry.Rect(400f, 0f, 800f, 400f), state.landing)
        }
        onNodeWithTag("handle-a").performMouseInput { release() }
        runOnIdle { assertEquals(listOf(MoruDropEdge.Center), moves) }
    }

    @Test
    fun droppingAHandleOnAnotherPanelNamesBothAndAShortDragOnItselfDoesNot() = runDesktopComposeUiTest(width = 800, height = 400) {
        val moves = mutableListOf<Triple<Any, Any, MoruDropEdge>>()
        lateinit var state: MoruPanelDragState
        setContent {
            MoruTheme {
                state = remember { MoruPanelDragState() }
                Row {
                    listOf("a", "b").forEach { key ->
                        Column(Modifier.size(400.dp, 400.dp).then(state.dropTarget(key))) {
                            Box(Modifier.fillMaxWidth().height(30.dp).testTag("handle-$key")
                                .then(state.handle(key) { from, to, edge -> moves += Triple(from, to, edge) }))
                        }
                    }
                }
            }
        }
        onNodeWithTag("handle-a").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(200f, 100f), delayMillis = 50)
            moveTo(center + Offset(400f, 200f), delayMillis = 50)
        }
        runOnIdle {
            assertEquals("b", state.target)
            assertEquals(MoruDropEdge.Center, state.edge)
        }
        onNodeWithTag("handle-a").performMouseInput { release() }
        runOnIdle {
            assertEquals(listOf(Triple<Any, Any, MoruDropEdge>("a", "b", MoruDropEdge.Center)), moves)
            assertNull(state.dragging)
        }

        onNodeWithTag("handle-b").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(60f, 80f), delayMillis = 50)
            release()
        }
        runOnIdle { assertEquals(1, moves.size) }

        // Near the right side of the other panel, it goes there instead of trading places, and the area it would
        // take is that panel's right half.
        onNodeWithTag("handle-a").performMouseInput {
            moveTo(center); press()
            moveTo(center + Offset(300f, 100f), delayMillis = 50)
            moveTo(center + Offset(580f, 185f), delayMillis = 50)
        }
        runOnIdle { assertEquals(androidx.compose.ui.geometry.Rect(600f, 0f, 800f, 400f), state.landing) }
        onNodeWithTag("handle-a").performMouseInput { release() }
        runOnIdle { assertNull(state.landing) }
        runOnIdle { assertEquals(Triple<Any, Any, MoruDropEdge>("a", "b", MoruDropEdge.Right), moves.last()) }
    }
}
