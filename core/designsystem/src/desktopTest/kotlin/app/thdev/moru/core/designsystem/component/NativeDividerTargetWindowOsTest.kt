package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.Component
import java.awt.Container
import java.awt.Frame
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeDividerTargetWindowOsTest {
    @Test
    fun anOverlayDividerTakesTheMouseAboveNativePanesOnBothSidesOfTheSeam() {
        var share by mutableStateOf(0.5f)
        val left = JPanel()
        val right = JPanel()
        NativePaneTestWindow {
            MoruTheme {
                MoruResizableShareSplitPane(
                    share, { share = it }, { share = 0.5f }, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    dividerOverlayModifier = Modifier.fillMaxHeight(),
                    left = { SwingPanel(ComposeColor.Red, { left }, it) },
                    right = { SwingPanel(ComposeColor.Blue, { right }, it) },
                )
            }
        }.use { window ->
            window.waitFor("native panes and their divider peer") { onEdt {
                val frame = SwingUtilities.getWindowAncestor(left) as? Frame
                left.isShowing && right.isShowing && left.width > 0 && right.width > 0 && frame != null &&
                    divider(frame)?.let { it.isShowing && it.width > 0 && it.height == left.height } == true
            } }
            val target = onEdt {
                // Window.getWindows() also retains disposed fixtures with the same title in a full suite.
                val frame = SwingUtilities.getWindowAncestor(left) as Frame
                val seam = SwingUtilities.convertPoint(left, left.width, 0, frame).x
                assertEquals(SwingUtilities.convertPoint(right, 0, 0, frame).x, seam, "No gutter between the panes")
                val top = SwingUtilities.convertPoint(left, 0, 4, frame).y
                val bottom = SwingUtilities.convertPoint(left, 0, left.height - 4, frame).y
                listOf(Point(seam - 2, top), Point(seam + 2, bottom), Point(seam, (top + bottom) / 2)).map { point ->
                    deepest(frame, point).also {
                        assertTrue(it is NativeDividerComponent, "Expected the divider at $point but found $it")
                    }
                }.last()!!
            }
            onEdt {
                fun send(id: Int, dx: Int) = target.dispatchEvent(MouseEvent(
                    target, id, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, 4 + dx, 100,
                    target.locationOnScreen.x + 4 + dx, target.locationOnScreen.y + 100, 1, false, MouseEvent.BUTTON1,
                ))
                send(MouseEvent.MOUSE_PRESSED, 0)
                send(MouseEvent.MOUSE_DRAGGED, -160)
                send(MouseEvent.MOUSE_RELEASED, -160)
            }
            window.waitFor("the drag to widen the right pane") { share > 0.65f }
        }
    }

    private fun divider(component: Component): NativeDividerComponent? =
        if (component is NativeDividerComponent) component
        else (component as? Container)?.components?.firstNotNullOfOrNull(::divider)

    private fun deepest(frame: Frame, point: Point): Component? {
        val root = (frame as RootPaneContainer).layeredPane
        val local = SwingUtilities.convertPoint(frame, point, root)
        return SwingUtilities.getDeepestComponentAt(root, local.x, local.y)
    }

    private fun <T> onEdt(read: () -> T): T {
        var value: Result<T>? = null
        SwingUtilities.invokeAndWait { value = runCatching(read) }
        return value!!.getOrThrow()
    }
}
