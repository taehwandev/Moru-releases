package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.GraphicsEnvironment
import java.awt.Desktop
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** AWT pointer events catch native window resizing without relying on global OS pointer ownership. */
class MoruPopoverHoverOsTest {
    @Test
    fun hoverAndPressKeepRowsAtTheirOriginalScreenCoordinates() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        // The notification inbox owns its scrolling; menus use the popover's scrolling.
        listOf(false, true).forEach { scrollable -> verifyPointerInput(scrollable) }
    }

    private fun verifyPointerInput(scrollable: Boolean) {
        var expanded by mutableStateOf(false)
        var clicks = 0
        val hovered = BooleanArray(4)
        var windowLevel: AutoCloseable? = null
        val owner = onEdt {
            ComposeWindow().apply {
                title = "Popover pointer regression"
                setSize(900, 700)
                setLocation(100, 100)
                prepareMoruNativeWindow(this)
                windowLevel = installPopoverFixtureWindowLevel(this)
                val window = this
                setContent {
                    MoruTheme {
                        MoruDesktopPopupHost(window) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.offset(60.dp, 30.dp).size(200.dp, 30.dp)) {
                                    MoruPopover(expanded, { expanded = false }, Modifier.width(320.dp), scrollable) {
                                        repeat(4) { index ->
                                            val source = remember { MutableInteractionSource() }
                                            val isHovered by source.collectIsHoveredAsState()
                                            hovered[index] = isHovered
                                            MoruText("Row $index", Modifier.fillMaxWidth()
                                                .clickable(interactionSource = source) { clicks++ }.padding(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                isVisible = true
                toFront()
                requestFocus()
                Desktop.getDesktop().requestForeground(true)
            }
        }
        try {
            waitFor("active pointer fixture") { onEdt { owner.isShowing && owner.isFocused } }
            onEdt { expanded = true }
            awaitRevealedPopup(owner)
            val before = onEdt { owner.ownedWindows.single { it.isShowing }.bounds }
            onEdt { owner.ownedWindows.single { it.isShowing }.toFront() }
            val popup = onEdt { owner.ownedWindows.single { it.isShowing } }
            assertTrue(before.height in 150..180, "Four compact rows: $before")
            repeat(3) {
                repeat(4) { index ->
                    postPointer(popup, MouseEvent.MOUSE_MOVED,
                        Point(before.x + 60, before.y + 2 + (index * 2 + 1) * (before.height - 4) / 8))
                    waitFor("pointer enters row $index") { onEdt { hovered[index] } }
                    assertEquals(before, onEdt { owner.ownedWindows.single { it.isShowing }.bounds },
                        "Hover must preserve native bounds (scrollable=$scrollable)")
                }
            }
            val clickPoint = Point(before.x + 60, before.y + 20)
            postPointer(popup, MouseEvent.MOUSE_MOVED, clickPoint)
            postPointer(popup, MouseEvent.MOUSE_PRESSED, clickPoint)
            Thread.sleep(180)
            assertEquals(before, onEdt { owner.ownedWindows.single { it.isShowing }.bounds }, "Press must preserve bounds")
            postPointer(popup, MouseEvent.MOUSE_RELEASED, clickPoint)
            waitFor("the first row receives its click") { onEdt { clicks == 1 } }
            assertEquals(1, onEdt { clicks }, "The original coordinates must still click the row")
            assertTrue(onEdt { expanded })
            assertEquals(before, onEdt { owner.ownedWindows.single { it.isShowing }.bounds })
        } finally {
            onEdt { owner.dispose(); windowLevel?.close() }
        }
    }

    private fun awaitRevealedPopup(owner: ComposeWindow) {
        var previous: Rectangle? = null
        var unchangedSince = System.nanoTime()
        // MoruPopover's 140 ms scale reveal changes Skia's recorded drawing bounds. The hover
        // contract starts after that opening animation; never stabilize again after pointer input.
        val revealQuietNanos = 200_000_000L
        waitFor("four popup rows finish their scale reveal") {
            val bounds = onEdt { owner.ownedWindows.singleOrNull { it.isShowing }?.bounds }
            val now = System.nanoTime()
            if (bounds != previous) {
                previous = bounds
                unchangedSince = now
            }
            bounds?.height in 150..180 && now - unchangedSince > revealQuietNanos
        }
    }

    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        error("Timed out waiting for $description")
    }

    private fun postPointer(popup: Window, id: Int, screenPoint: Point) = onEdt {
        fun target(component: Component): Component? =
            if (component.mouseMotionListeners.isNotEmpty()) component
            else (component as? Container)?.components?.firstNotNullOfOrNull(::target)
        val component = checkNotNull(target(popup)) { "Popup must have a native pointer input target" }
        val local = Point(screenPoint).apply { SwingUtilities.convertPointFromScreen(this, component) }
        val button = if (id == MouseEvent.MOUSE_MOVED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1
        val modifiers = if (id == MouseEvent.MOUSE_PRESSED) InputEvent.BUTTON1_DOWN_MASK else 0
        component.dispatchEvent(MouseEvent(component, id, System.currentTimeMillis(), modifiers,
            local.x, local.y, screenPoint.x, screenPoint.y, if (button == MouseEvent.BUTTON1) 1 else 0, false, button))
    }

    private fun <T> onEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }
}
