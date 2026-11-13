package app.thdev.moru.core.designsystem.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.moruAmbientBackground
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import org.jetbrains.skiko.SkiaLayer
import org.junit.Assume.assumeTrue
import java.awt.Component
import java.awt.Container
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** A native class alone is insufficient: stationary material must respond to its live backdrop. */
class MacBackdropNativeOsTest {
    @Test
    fun disposedWindowAndUnknownNativeHandlesReturnNoHit() {
        assumeTrue(System.getProperty("os.name").startsWith("Mac") && !GraphicsEnvironment.isHeadless())
        val api = assertNotNull(MacBackdropBridge.api)
        val frame = onEdt { ComposeWindow().apply { setSize(120, 100); isVisible = true } }
        val handle = try {
            onEdt { assertNotNull(findLayer(frame)).windowHandle }
        } finally {
            onEdt { frame.dispose() }
        }
        assertEquals(0, api.moru_backdrop_probe(handle, 1.0, 1.0))
        assertEquals(0, api.moru_backdrop_probe(1, 1.0, 1.0))
    }

    @Test
    fun nativeChromeAndOpaquePopupKeepTheirRenderingAndReleaseOwnedViews() {
        assumeTrue(System.getProperty("os.name").startsWith("Mac") && !GraphicsEnvironment.isHeadless())
        val api = assertNotNull(MacBackdropBridge.api, "The bundled native bridge must load on macOS")
        val baseline = api.moru_backdrop_count()
        val expanded = mutableStateOf(false)
        val pageFill = mutableStateOf(Color.Transparent)
        val dark = mutableStateOf(false)
        val popupWidth = mutableStateOf(260)
        val darkPopupReady = AtomicBoolean()
        val popupUsesNative = AtomicBoolean(true)
        val pointerClicks = java.util.concurrent.atomic.AtomicInteger()
        val background = JPanel().apply { this.background = java.awt.Color(235, 65, 55) }
        var behind: JFrame? = null
        var front: ComposeWindow? = null
        val robot = Robot().apply { autoDelay = 50 }
        try {
            onEdt {
                behind = JFrame().apply {
                    isUndecorated = true
                    contentPane = background
                    setBounds(80, 80, 760, 560)
                    isVisible = true
                }
                front = ComposeWindow().apply {
                    prepareMoruNativeWindow(this)
                    title = "Native backdrop verification"
                    setBounds(160, 150, 520, 360)
                    isAlwaysOnTop = true
                    val owner = this
                    setContent {
                        MoruDesktopPopupHost(owner, materialStyle = MoruMaterialStyle.Apple, darkTheme = dark.value) {
                            MoruTheme(darkTheme = dark.value) {
                                Box(Modifier.fillMaxSize().moruAmbientBackground().background(pageFill.value)) {
                                    MoruText("Native backdrop")
                                    SwingPanel(
                                        factory = { JPanel().apply { this.background = java.awt.Color.RED } },
                                        background = Color.Red,
                                        modifier = Modifier.offset(430.dp, 0.dp).width(90.dp).fillMaxHeight(),
                                    )
                                    Box(Modifier.offset(360.dp, 240.dp).size(24.dp).background(Color.Magenta).clickable(
                                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                    ) { pointerClicks.incrementAndGet() })
                                    Box(Modifier.offset(80.dp, 60.dp)) {
                                        MoruPopover(expanded.value, onDismiss = { expanded.value = false }) {
                                            val native = LocalMoruNativeMaterial.current
                                            SideEffect {
                                                popupUsesNative.set(native)
                                                darkPopupReady.set(dark.value && !native)
                                            }
                                            Box(Modifier.size(popupWidth.value.dp, 190.dp)) {
                                                MoruText("Popup native material: ${LocalMoruNativeMaterial.current}")
                                                Box(Modifier.offset(180.dp, 30.dp).size(24.dp).background(Color.Green))
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
                    java.awt.Desktop.getDesktop().requestForeground(true)
                }
            }
            await("main material installation") { api.moru_backdrop_count() == baseline + 1 }
            val frame = checkNotNull(front)
            await("active native fixture") { onEdt { frame.isFocused } }
            val clickPoint = onEdt { java.awt.Point(frame.x + 372, frame.y + frame.insets.top + 252) }
            val input = MoruNativePointerProbe.inspect(frame, clickPoint)
            assertTrue(input.windowSelected && input.contentSelected, "Native mouse-down must target the existing AWT foreground")
            if (input.eventPostingAllowed) {
                robot.mouseMove(clickPoint.x, clickPoint.y)
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            } else {
                println("macOS denies synthetic mouse posting; validating native hit target plus AWT event delivery")
                onEdt {
                    val local = java.awt.Point(clickPoint)
                    SwingUtilities.convertPointFromScreen(local, frame)
                    val target = SwingUtilities.getDeepestComponentAt(frame, local.x, local.y)!!
                    val point = SwingUtilities.convertPoint(frame, local, target)
                    for (id in listOf(java.awt.event.MouseEvent.MOUSE_PRESSED, java.awt.event.MouseEvent.MOUSE_RELEASED)) {
                        target.dispatchEvent(java.awt.event.MouseEvent(target, id, System.currentTimeMillis(),
                            if (id == java.awt.event.MouseEvent.MOUSE_PRESSED) InputEvent.BUTTON1_DOWN_MASK else 0,
                            point.x, point.y, clickPoint.x, clickPoint.y, 1, false, java.awt.event.MouseEvent.BUTTON1))
                    }
                }
            }
            await("Compose pointer input above the material") { pointerClicks.get() == 1 }
            val chromeBounds = onEdt { Rectangle(frame.x + 30, frame.y + 160, 150, 100) }
            val chromeRed = capture(robot, chromeBounds, "chrome-red")
            val chromeScene = capture(robot, Rectangle(80, 80, 760, 560), "scene-chrome")
            assertTrue(hasColor(chromeScene, 0xFF00FF), "Compose foreground must stay visible above the native material")
            assertTrue(hasColor(chromeScene, 0xFF0000), "Opaque Swing foreground must remain visible beside native chrome")
            onEdt { background.background = java.awt.Color(35, 95, 235); background.repaint() }
            val chromeBlue = capture(robot, chromeBounds, "chrome-blue")
            assertTrue(meanDifference(chromeRed, chromeBlue) > 5.0, "Chrome must sample its external backdrop")

            onEdt { pageFill.value = Color(0xFFDF4438); expanded.value = true }
            await("opaque popup above the retained main material") {
                !popupUsesNative.get() && api.moru_backdrop_count() == baseline + 1 && onEdt { frame.ownedWindows.any { it.isShowing } }
            }
            val popup = onEdt { frame.ownedWindows.last { it.isShowing } }
            onEdt {
                java.awt.Desktop.getDesktop().requestForeground(true)
                popup.toFront()
            }
            await("active popup owner") { onEdt { frame.isFocused || popup.isFocused } }
            val popupBounds = onEdt { Rectangle(popup.x + 40, popup.y + 90, 150, 70) }
            val popupRed = capture(robot, popupBounds, "popup-red")
            val redScene = capture(robot, Rectangle(80, 80, 760, 560), "scene-red")
            assertTrue(hasColor(redScene, 0x00FF00), "Popup foreground must stay visible above its opaque backing")
            assertTrue(hasColor(redScene, 0xDF4438), "The live parent backdrop must show the requested red content")
            onEdt { pageFill.value = Color(0xFF2563DD) }
            val popupBlue = capture(robot, popupBounds, "popup-blue")
            capture(robot, Rectangle(80, 80, 760, 560), "scene-blue")
            File("build/native-backdrop/geometry.txt").writeText(onEdt {
                "main=${frame.bounds}; popup=${popup.bounds}; sample=$popupBounds; difference=${meanDifference(popupRed, popupBlue)}"
            })
            assertTrue(meanDifference(popupRed, popupBlue) < 1.0, "The unbacked popup must stay opaque over changing parent content")
            onEdt { dark.value = true }
            await("dark popup appearance without an extra native attachment") { darkPopupReady.get() && api.moru_backdrop_count() == baseline + 1 }
            val popupDark = capture(robot, popupBounds, "popup-dark")
            assertTrue(meanDifference(popupBlue, popupDark) > 5.0, "Popup appearance must follow the app theme")
            val oldPopupWidth = onEdt { popup.width }
            onEdt { popupWidth.value = 320 }
            await("popup content resize") { onEdt { popup.width > oldPopupWidth + 40 } }
            val extendedBounds = onEdt { Rectangle(popup.x + oldPopupWidth + 15, popup.y + 90, 12, 30) }
            val extension = capture(robot, extendedBounds, "popup-resized-extension")
            assertTrue(!hasColor(extension, 0x2563DD), "Opaque backing must cover the new popup extent")
            onEdt { frame.setSize(600, 400) }
            assertTrue(onEdt { popup.isShowing }, "Resize must retain the open popup")
            onEdt { expanded.value = false }
            await("popup dismissal retains the main material") {
                api.moru_backdrop_count() == baseline + 1 && onEdt { frame.ownedWindows.none { it.isShowing } }
            }
            onEdt { frame.dispose(); front = null }
            await("main material release") { api.moru_backdrop_count() == baseline }
        } finally {
            onEdt { front?.dispose(); behind?.dispose() }
            await("native material cleanup") { api.moru_backdrop_count() == baseline }
        }
    }

    private fun capture(robot: Robot, bounds: Rectangle, name: String): BufferedImage {
        Thread.sleep(350)
        val image = robot.createScreenCapture(bounds)
        val output = File("build/native-backdrop/$name.png")
        output.parentFile.mkdirs()
        ImageIO.write(image, "png", output)
        return image
    }

    private fun meanDifference(first: BufferedImage, second: BufferedImage): Double {
        var sum = 0L
        for (y in 0 until first.height) for (x in 0 until first.width) {
            val a = first.getRGB(x, y)
            val b = second.getRGB(x, y)
            for (shift in listOf(0, 8, 16)) sum += kotlin.math.abs(((a shr shift) and 255) - ((b shr shift) and 255))
        }
        return sum.toDouble() / (first.width * first.height * 3)
    }

    private fun hasColor(image: BufferedImage, rgb: Int): Boolean {
        var matches = 0
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val actual = image.getRGB(x, y)
            // Native color management can shift a channel by a few levels; cream/hidden content still fails.
            if (listOf(0, 8, 16).all { shift ->
                    kotlin.math.abs(((actual shr shift) and 255) - ((rgb shr shift) and 255)) <= 8
                }) matches++
        }
        return matches >= 100
    }

    private fun findLayer(component: Component): SkiaLayer? =
        if (component is SkiaLayer) component else (component as? Container)?.components?.firstNotNullOfOrNull(::findLayer)

    private fun await(description: String, read: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (read()) return
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
