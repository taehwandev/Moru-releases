package app.thdev.moru.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.feature.workbench.TerminalCloseConfirm
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

/** The close alert dims native terminals behind it instead of hiding them or sliding beneath them. */
class TerminalCloseAlertNativeWindowOsTest {
    @Test fun `the close alert dims the native terminals and its card sits above them`() {
        val open = mutableStateOf(false)
        val left = JPanel().apply { background = java.awt.Color.RED }
        val right = JPanel().apply { background = java.awt.Color.RED }
        val frame = onEdt {
            ComposeWindow().apply {
                title = "Close alert native window"
                setSize(1000, 680)
                setLocation(80, 80)
                isAlwaysOnTop = true
                val owner = this
                setContent {
                    MoruDesktopPopupHost(owner) {
                        MoruTheme(colors = darkCkColors) {
                            // Two native terminals side by side, as in a split workspace.
                            Row(Modifier.fillMaxSize()) {
                                SwingPanel(factory = { left }, background = Color.Red, modifier = Modifier.weight(1f).fillMaxHeight())
                                SwingPanel(factory = { right }, background = Color.Red, modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                            if (open.value) TerminalCloseConfirm(onConfirm = {}, onKeepOpen = { open.value = false },
                                shortcut = "⌘W", title = "AGY", reopenShortcut = "⇧⌘T")
                        }
                    }
                }
                isVisible = true
                toFront()
            }
        }
        try {
            waitFor("terminals") { onEdt { left.isShowing && right.isShowing } }
            Thread.sleep(500)
            val robot = Robot()
            val corner = onEdt { Point(right.locationOnScreen.x + right.width - 30, right.locationOnScreen.y + right.height - 30) }
            assertTrue(isRed(robot.getPixelColor(corner.x, corner.y)), "The terminal is plain red before the alert")
            onEdt { open.value = true }
            // The terminal behind is still red, only darker: dimmed, not hidden behind an opaque layer.
            waitFor("dimmed terminal") { robot.getPixelColor(corner.x, corner.y).let { it.red in 60..200 && it.green < 40 && it.blue < 40 } }
            Thread.sleep(400)
            val middle = onEdt { Point(frame.x + frame.width / 2, frame.y + frame.height / 2) }
            assertTrue(!isRed(robot.getPixelColor(middle.x, middle.y)), "The card covers the middle above both terminals")
            val capture = robot.createScreenCapture(onEdt { Rectangle(frame.locationOnScreen, frame.size) })
            File("build/ui-captures").mkdirs()
            ImageIO.write(capture, "png", File("build/ui-captures/terminal-close-alert-native.png"))
        } finally {
            onEdt { frame.dispose() }
        }
    }

    private fun isRed(color: java.awt.Color) = color.red > 200 && color.green < 60 && color.blue < 60

    private fun waitFor(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        error("Timed out waiting for $description")
    }

    private fun <T> onEdt(action: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return action()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(action) }
        return checkNotNull(result).getOrThrow()
    }
}
