package app.thdev.moru.core.designsystem.component

import androidx.compose.ui.Alignment
import java.awt.GraphicsEnvironment
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Native window bounds expose shadow gutters that a Compose semantics rectangle cannot see. */
class MoruPopoverNativeWindowOsTest {
    @Test
    fun compactAndOversizedPopoversStayWithinTheirActualOwnerAtEveryEdge() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        listOf(false, true).forEach { dark ->
            PopoverWindowFixture(dark).use { fixture ->
                listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { edge ->
                    fixture.open(edge)
                    fixture.waitFor("compact popup without native shadow gutters") {
                        fixture.onEdt { fixture.popup()?.let { it.height in 42..46 && fixture.contentBounds().contains(it.bounds) } == true }
                    }
                    fixture.onEdt {
                        val popup = checkNotNull(fixture.popup())
                        assertTrue(popup.width in 180..184, "180dp content with at most two antialias dp per edge: ${popup.bounds}")
                        assertEquals(fixture.owner, popup.owner)
                    }
                    fixture.hide()
                }
                fixture.open(Alignment.BottomEnd, oversized = true)
                fixture.waitFor("oversized content bounded by its native owner") {
                    fixture.onEdt {
                        val popup = fixture.popup()
                        val available = fixture.contentBounds()
                        popup != null && available.contains(popup.bounds) && popup.width == available.width && popup.height <= available.height
                    }
                }
            }
        }
    }
}
