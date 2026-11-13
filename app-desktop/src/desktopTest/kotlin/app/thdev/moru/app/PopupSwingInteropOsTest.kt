package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.material.MoruNativePointerProbe
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.SwingUtilities
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Real pixels and native hit targets detect Swing occlusion; input posting also respects macOS permission. */
class PopupSwingInteropOsTest {
    @Test
    fun aPopoverDrawsOverSwingAndSupportsSelectionEscapeAndOutsideClick() {
        assumeFalse("A Swing popup regression requires a display", GraphicsEnvironment.isHeadless())
        val configuredKeys = listOf(
            MoruDesktopRuntimeProperties.AppleApplicationNameProperty,
            MoruDesktopRuntimeProperties.ComposeLayersTypeProperty,
        )
        val previousProperties = configuredKeys.associateWith(System::getProperty)
        // Configure before constructing any Compose UI, just as Main does.
        MoruDesktopRuntimeProperties.configure()
        val popupPolicy = MoruPopupWindowPolicy.install()
        val expanded = mutableStateOf(false)
        var selections = 0
        val swing = JPanel().apply {
            background = java.awt.Color.RED
            isFocusable = true
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) { requestFocusInWindow() }
            })
        }
        var frame: ComposeWindow? = null
        var diagnosticRobot: Robot? = null
        try {
            val robot = Robot().apply { autoDelay = 40 }
            diagnosticRobot = robot
            onEdt {
                frame = ComposeWindow().apply {
                    title = "Moru popup interop regression"
                    setSize(640, 420)
                    setLocation(40, 40)
                    // Match production ownership/levels: an always-on-top parent can cover its normal-level menu.
                    isAlwaysOnTop = false
                    prepareMoruNativeWindow(this)
                    val owner = this
                    setContent {
                        MoruDesktopPopupHost(owner) {
                        MoruTheme {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxWidth().height(48.dp)) {
                                    Box(Modifier.offset(x = 280.dp).size(64.dp, 48.dp).clickable { expanded.value = true }) {
                                        MoruPopover(
                                            expanded = expanded.value,
                                            onDismiss = { expanded.value = false },
                                            modifier = Modifier.width(240.dp),
                                        ) {
                                            Box(Modifier.size(220.dp, 104.dp).background(Color.Cyan).clickable {
                                                selections += 1
                                                expanded.value = false
                                            })
                                        }
                                    }
                                }
                                SwingPanel(factory = { swing }, background = Color.Red, modifier = Modifier.fillMaxWidth().weight(1f))
                            }
                        }
                        }
                    }
                    isVisible = true
                    toFront()
                    requestFocus()
                    java.awt.Desktop.getDesktop().requestForeground(true)
                }
            }
            waitFor("Swing content to appear") { onEdt { swing.isShowing && swing.width > 100 } }
            waitFor("the fixture to receive native focus") { onEdt { checkNotNull(frame).isFocused } }

            fun popupPoint(): Point? {
                val bounds = onEdt { Rectangle(swing.locationOnScreen, swing.size) }
                val image = robot.createScreenCapture(bounds)
                var count = 0
                var left = image.width
                var right = 0
                var top = image.height
                var bottom = 0
                for (y in 0 until image.height step 4) {
                    for (x in 0 until image.width step 4) {
                        val rgb = image.getRGB(x, y)
                        // macOS display color conversion renders Color.Cyan as 03FFFF.
                        if (((rgb shr 16) and 0xFF) < 32 && ((rgb shr 8) and 0xFF) > 224 && (rgb and 0xFF) > 224) {
                            count += 1
                            left = minOf(left, x)
                            right = maxOf(right, x)
                            top = minOf(top, y)
                            bottom = maxOf(bottom, y)
                        }
                    }
                }
                // A substantial cyan area must cover the actual red Swing surface.
                return if (count >= 100) Point(bounds.x + (left + right) / 2, bounds.y + (top + bottom) / 2) else null
            }
            fun open(): Point {
                if (!onEdt { checkNotNull(frame).isFocused }) {
                    onEdt {
                        checkNotNull(frame).toFront()
                        checkNotNull(frame).requestFocus()
                        java.awt.Desktop.getDesktop().requestForeground(true)
                    }
                    waitFor("the fixture to regain focus") { onEdt { checkNotNull(frame).isFocused } }
                }
                val anchorPoint = onEdt { Point(checkNotNull(frame).x + 312, swing.locationOnScreen.y - 24) }
                click(robot, anchorPoint, checkNotNull(frame))
                waitFor("the menu to receive native focus") {
                    onEdt { checkNotNull(frame).ownedWindows.any { it.isShowing && it.isFocused } }
                }
                // Keep the coordinate from the successful capture; a second capture can land between frames.
                val point = waitForValue("cyan popup pixels over the red Swing surface", ::popupPoint)
                onEdt {
                    val popup = checkNotNull(frame).ownedWindows.single { it.isShowing }
                    assertEquals(frame, popup.owner)
                    assertFalse(popup.isAlwaysOnTop, "A Moru menu must not float over other applications")
                }
                return point
            }
            fun assertDismissed() = waitFor("the popup window to finish closing") {
                onEdt { !expanded.value && checkNotNull(frame).ownedWindows.none { it.isShowing } } && popupPoint() == null
            }

            val popupSelectionPoint = open()
            val capture = robot.createScreenCapture(onEdt { checkNotNull(frame).bounds })
            val artifact = File("build/popup-interop/popover-over-swing.png")
            artifact.parentFile.mkdirs()
            ImageIO.write(capture, "png", artifact)
            click(robot, popupSelectionPoint, checkNotNull(frame))
            assertDismissed()
            assertEquals(1, onEdt { selections }, "A click over Swing must select the popup item")

            open()
            waitFor("the menu to receive keyboard focus") { onEdt { checkNotNull(frame).ownedWindows.any { it.isShowing && it.isFocused } } }
            val menu = onEdt { checkNotNull(frame).ownedWindows.single { it.isShowing } }
            val menuPoint = onEdt { Point(menu.x + menu.width / 2, menu.y + menu.height / 2) }
            if (MoruNativePointerProbe.inspect(menu, menuPoint).eventPostingAllowed) {
                robot.keyPress(KeyEvent.VK_ESCAPE)
                robot.keyRelease(KeyEvent.VK_ESCAPE)
            } else onEdt {
                menu.dispatchEvent(KeyEvent(menu, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED))
            }
            assertDismissed()
            assertEquals(1, onEdt { selections })

            open()
            click(robot, onEdt { Point(swing.locationOnScreen.x + 20, swing.locationOnScreen.y + swing.height - 20) }, checkNotNull(frame))
            assertDismissed()
            assertEquals(1, onEdt { selections })
            waitFor("pointer input to return focus to Swing") { onEdt { swing.isFocusOwner } }
        } catch (failure: Throwable) {
            runCatching {
                val robot = checkNotNull(diagnosticRobot)
                val owner = checkNotNull(frame)
                val frameBounds = onEdt { owner.bounds }
                val swingBounds = onEdt { if (swing.isShowing) Rectangle(swing.locationOnScreen, swing.size) else null }
                val frameImage = robot.createScreenCapture(frameBounds)
                val artifacts = File("build/popup-interop").apply { mkdirs() }
                ImageIO.write(frameImage, "png", File(artifacts, "failure-frame.png"))
                println("popup-interop frame=$frameBounds swing=$swingBounds visible=${onEdt { swing.isVisible }} showing=${onEdt { swing.isShowing }}")
                println("popup-interop expanded=${onEdt { expanded.value }} active=${onEdt { owner.isActive }} focused=${onEdt { owner.isFocused }} swingFocus=${onEdt { swing.isFocusOwner }}")
                println("popup-interop owned=${onEdt { owner.ownedWindows.map { window -> "${window.bounds} showing=${window.isShowing} focused=${window.isFocused} active=${window.isActive} top=${window.isAlwaysOnTop}" } }}")
                println("popup-interop layers=${System.getProperty(MoruDesktopRuntimeProperties.ComposeLayersTypeProperty)}")
                println("popup-interop frame colors=${majorColors(frameImage)} swing colors=${swingBounds?.let { majorColors(robot.createScreenCapture(it)) }}")
            }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        } finally {
            frame?.let { owner -> onEdt { owner.dispose() } }
            popupPolicy.close()
            previousProperties.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
        }
    }

    private fun majorColors(image: BufferedImage): String {
        val counts = mutableMapOf<Int, Int>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y) and 0xFFFFFF
                counts[rgb] = (counts[rgb] ?: 0) + 1
            }
        }
        return counts.entries.sortedByDescending { it.value }.take(5)
            .joinToString { (rgb, count) -> "%06X=%d".format(rgb, count) }
    }

    private fun click(robot: Robot, point: Point, owner: java.awt.Window) {
        val targetWindow = onEdt { owner.ownedWindows.firstOrNull { it.isShowing && it.bounds.contains(point) } ?: owner }
        val native = MoruNativePointerProbe.inspect(targetWindow, point)
        kotlin.test.assertTrue(native.windowSelected && native.contentSelected, "Native mouse-down must reach the intended AWT foreground")
        if (native.eventPostingAllowed) {
            robot.mouseMove(point.x, point.y)
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        } else onEdt {
            println("macOS denies synthetic mouse posting; validating native hit target plus AWT event delivery")
            targetWindow.toFront()
            targetWindow.requestFocus()
            val local = Point(point)
            SwingUtilities.convertPointFromScreen(local, targetWindow)
            val target = SwingUtilities.getDeepestComponentAt(targetWindow, local.x, local.y)!!
            val relative = SwingUtilities.convertPoint(targetWindow, local, target)
            for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                target.dispatchEvent(MouseEvent(target, id, System.currentTimeMillis(),
                    if (id == MouseEvent.MOUSE_PRESSED) InputEvent.BUTTON1_DOWN_MASK else 0,
                    relative.x, relative.y, point.x, point.y, 1, false, MouseEvent.BUTTON1))
            }
        }
    }

    private fun waitFor(what: String, read: () -> Boolean) {
        waitForValue(what) { true.takeIf { read() } }
    }

    private fun <T : Any> waitForValue(what: String, read: () -> T?): T {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            read()?.let { return it }
            Thread.sleep(50)
        }
        error("Timed out waiting for $what")
    }

    private fun <T> onEdt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }
}
