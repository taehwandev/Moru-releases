package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.Color
import java.awt.Frame
import java.awt.Point
import java.awt.Robot
import java.awt.Window
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals

/** The divider target as the screen shows it. Needs the machine to itself. */
class NativeDividerTargetOsTest {
    @Test
    fun theDividerTargetPaintsNothingOverTheNativePanesBesideTheSeam() {
        val left = JPanel()
        val right = JPanel()
        NativePaneTestWindow {
            MoruTheme {
                MoruResizableShareSplitPane(
                    0.5f, {}, {}, "Resize panes", "Reset pane size", 300.dp, 240.dp,
                    dividerOverlayModifier = Modifier.fillMaxHeight(),
                    left = { SwingPanel(ComposeColor.Red, { left }, it) },
                    right = { SwingPanel(ComposeColor.Blue, { right }, it) },
                )
            }
        }.use { window ->
            window.waitFor("both native panes") { onEdt { left.isShowing && right.isShowing && left.width > 0 } }
            onEdt { Window.getWindows().filterIsInstance<Frame>().first { it.title == "Pane fixture" }.apply { isAlwaysOnTop = true; toFront() } }
            Thread.sleep(800)
            // The target paints nothing, so both panes still show their own pixels right beside the seam.
            val (onLeft, onRight) = onEdt {
                val origin = left.locationOnScreen
                Point(origin.x + left.width - 2, origin.y + left.height / 2) to
                    Point(origin.x + left.width + 2, origin.y + left.height / 2)
            }
            val robot = Robot()
            assertEquals(Color.RED.rgb, robot.getPixelColor(onLeft.x, onLeft.y).rgb, "Left pane under the target")
            assertEquals(Color.BLUE.rgb, robot.getPixelColor(onRight.x, onRight.y).rgb, "Right pane under the target")
        }
    }

    private fun <T> onEdt(read: () -> T): T {
        var value: Result<T>? = null
        SwingUtilities.invokeAndWait { value = runCatching(read) }
        return value!!.getOrThrow()
    }
}
