package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test

class MoruDetachablePaneResizeOsTest {
    /** Hosted inside another Compose window, the pane's own root is almost unbounded; content must still fit its slot. */
    @Test
    fun dockedPaneLaysOutAtItsSlotSize() {
        var width by mutableStateOf(600)
        val parentWidth = AtomicReference(-1)
        val appliedWidth = AtomicReference(-1)
        val slotWidth = AtomicReference(-1)
        val endRight = AtomicReference(-1f)
        NativePaneTestWindow {
            Box(Modifier.width(width.dp).fillMaxHeight().onGloballyPositioned {
                parentWidth.set(it.size.width)
                appliedWidth.set(width)
            }) {
                MoruDetachablePane(false, {}, "Pane", Modifier.width(width.dp).fillMaxHeight(), placeholder = {}) { slot ->
                    Box(slot.onGloballyPositioned { slotWidth.set(it.size.width) }) {
                        Text("Docked content")
                        Box(Modifier.align(Alignment.TopEnd).size(24.dp).onGloballyPositioned {
                            endRight.set(it.positionInRoot().x + it.size.width)
                        })
                    }
                }
            }
        }.use { window ->
            window.waitFor("docked content") { window.exists("Pane fixture", "Docked content") }
            for (next in listOf(600, 421, 303, 520, 600, 303)) {
                window.runOnEdt { width = next }
                // Pixels at the fixture's density; the right-aligned child must end at the slot's right edge.
                window.waitFor("content laid out at the $next dp slot") {
                    val laidOut = slotWidth.get()
                    appliedWidth.get() == next && laidOut > 0 && laidOut == parentWidth.get() &&
                        endRight.get() == laidOut.toFloat()
                }
            }
        }
    }
}
