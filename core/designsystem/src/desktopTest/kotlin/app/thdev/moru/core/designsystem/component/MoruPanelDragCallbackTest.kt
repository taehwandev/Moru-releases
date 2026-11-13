package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalTestApi::class)
class MoruPanelDragCallbackTest {
    @Test
    fun droppingAfterLayoutChangesUsesTheLatestCallbackWithoutCancellingTheDrag() =
        runDesktopComposeUiTest(width = 800, height = 400) {
            val revision = mutableStateOf(0)
            val moves = mutableListOf<Int>()
            setContent {
                MoruTheme {
                    val state = remember { MoruPanelDragState() }
                    val layoutRevision = revision.value
                    Row {
                        listOf("a", "b").forEach { key ->
                            Column(Modifier.size(400.dp, 400.dp).then(state.dropTarget(key))) {
                                Box(Modifier.fillMaxWidth().height(30.dp).testTag("handle-$key")
                                    .then(state.handle(key) { _, _, _ -> moves += layoutRevision }))
                            }
                        }
                    }
                }
            }
            // A seam resize changes the layout captured by the drop callback while handle keys stay the same.
            runOnIdle { revision.value = 1 }
            onNodeWithTag("handle-a").performMouseInput {
                moveTo(center); press()
                moveTo(center + Offset(200f, 100f), delayMillis = 50)
                moveTo(center + Offset(400f, 200f), delayMillis = 50)
            }
            // Metadata/layout can also redraw while the pointer is held; the gesture must remain active.
            runOnIdle { revision.value = 2 }
            onNodeWithTag("handle-a").performMouseInput { release() }
            runOnIdle { assertEquals(listOf(2), moves) }
        }
}
