package app.thdev.moru.core.designsystem.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.component.MoruDetachablePane
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.LocalMoruNativeMaterial
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import org.junit.Assume.assumeTrue
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Exercises the separate SwingGraphics composition used by the real right Result sidebar. */
class DetachableBackdropOsTest {
    @Test
    fun dockedAndDetachedPanesExposeTheirLiveMaterialWithoutLosingForeground() {
        assumeTrue(System.getProperty("os.name").startsWith("Mac") && !GraphicsEnvironment.isHeadless())
        val api = assertNotNull(MacBackdropBridge.api)
        val baseline = api.moru_backdrop_count()
        val detached = mutableStateOf(false)
        val style = mutableStateOf(MoruMaterialStyle.Apple)
        val dark = mutableStateOf(false)
        val paneNative = AtomicBoolean()
        val background = JPanel().apply { this.background = java.awt.Color(235, 65, 55) }
        val robot = Robot()
        var behind: JFrame? = null
        var front: ComposeWindow? = null
        try {
            onEdt {
                behind = JFrame().apply {
                    isUndecorated = true
                    contentPane = background
                    setBounds(80, 80, 900, 700)
                    isVisible = true
                }
                front = createWindow(detached, style, dark, paneNative)
            }
            val main = checkNotNull(front)
            await("docked native context") { paneNative.get() && api.moru_backdrop_count() == baseline + 1 }
            assertBackdrop(robot, main, background, "docked")

            val paletteBounds = onEdt { Rectangle(main.x + 40, main.y + 160, 150, 70) }
            val lightPalette = capture(robot, paletteBounds, "shared-light")
            onEdt { dark.value = true }
            await("shared dark palette") { paneNative.get() && api.moru_backdrop_count() == baseline + 1 }
            // Material follows the window's active state. Reestablish the same active, live blue control
            // after replacement before comparing palettes, while checking foreground and lease ownership too.
            assertBackdrop(robot, main, background, "shared-dark-live")
            assertTrue(meanDifference(lightPalette, capture(robot, paletteBounds, "shared-dark")) > 5.0,
                "Changing the palette must update the existing native material while both docked hosts retain leases")
            onEdt { dark.value = false }
            await("shared light palette restored") { paneNative.get() && api.moru_backdrop_count() == baseline + 1 }
            assertBackdrop(robot, main, background, "shared-light-restored-live")
            assertTrue(meanDifference(lightPalette, capture(robot, paletteBounds, "shared-light-restored")) < 2.0,
                "Returning to light must restore the material without accumulating native views")

            onEdt { detached.value = true }
            await("detached native material") {
                paneNative.get() && api.moru_backdrop_count() == baseline + 2 && floatingWindow() != null
            }
            val floating = onEdt { checkNotNull(floatingWindow()).apply {
                // Sample the same controlled backdrop, clear of the main window's other content.
                setBounds(700, 180, 520, 360)
                isAlwaysOnTop = true
                toFront()
                requestFocus()
                java.awt.Desktop.getDesktop().requestForeground(true)
            } }
            await("active detached fixture") { onEdt { floating.isFocused } }
            assertBackdrop(robot, floating, background, "detached")
            onEdt { style.value = MoruMaterialStyle.Standard }
            await("standard style releases both materials") { !paneNative.get() && api.moru_backdrop_count() == baseline }
            val standardBounds = onEdt { Rectangle(floating.x + 40, floating.y + 160, 150, 70) }
            val standard = capture(robot, standardBounds, "standard-blue")
            onEdt { background.background = java.awt.Color.RED; background.repaint() }
            assertTrue(meanDifference(standard, capture(robot, standardBounds, "standard-red")) < 1.0,
                "Standard surfaces must stay opaque when the external backdrop changes")
            onEdt { style.value = MoruMaterialStyle.Apple }
            await("Apple style reinstalls both materials") { paneNative.get() && api.moru_backdrop_count() == baseline + 2 }

            onEdt { detached.value = false }
            await("docked material after restore") { paneNative.get() && api.moru_backdrop_count() == baseline + 1 }
            assertBackdrop(robot, main, background, "restored")
            onEdt { main.dispose(); front = null }
            await("all pane materials released") { api.moru_backdrop_count() == baseline }
        } finally {
            onEdt { front?.dispose(); behind?.dispose() }
            await("native cleanup") { api.moru_backdrop_count() == baseline }
        }
    }

    private fun createWindow(
        detached: MutableState<Boolean>, style: MutableState<MoruMaterialStyle>, dark: MutableState<Boolean>, native: AtomicBoolean,
    ) = ComposeWindow().apply {
        prepareMoruNativeWindow(this)
        title = "Docked material fixture"
        setBounds(160, 150, 520, 360)
        isAlwaysOnTop = true
        val owner = this
        setContent {
            MoruDesktopPopupHost(owner, materialStyle = style.value, darkTheme = dark.value) {
                MoruTheme(darkTheme = dark.value) {
                    MoruDetachablePane(detached.value, { detached.value = false }, FLOATING_TITLE, placeholder = {}) { slot ->
                        val installed = LocalMoruNativeMaterial.current
                        SideEffect { native.set(installed) }
                        Box(slot.ckSurface(MoruSurfaceRole.Panel)) {
                            MoruText("Result sidebar")
                            Box(Modifier.offset(280.dp, 80.dp).size(24.dp).background(Color.Magenta))
                        }
                    }
                }
            }
        }
        isVisible = true
        toFront()
        java.awt.Desktop.getDesktop().requestForeground(true)
    }

    private fun assertBackdrop(robot: Robot, window: Window, background: JPanel, name: String) {
        onEdt {
            background.background = java.awt.Color(235, 65, 55)
            background.repaint()
            window.toFront()
            window.requestFocus()
            java.awt.Desktop.getDesktop().requestForeground(true)
        }
        await("active $name fixture") { onEdt { window.isFocused } }
        val bounds = onEdt { Rectangle(window.x + 40, window.y + 160, 150, 70) }
        val red = capture(robot, bounds, "$name-red")
        capture(robot, onEdt { Rectangle(window.bounds) }, "$name-scene")
        onEdt { background.background = java.awt.Color(35, 95, 235); background.repaint() }
        val blue = capture(robot, bounds, "$name-blue")
        val difference = meanDifference(red, blue)
        assertTrue(difference > 5.0, "$name Result pane must expose live material; pixel difference=$difference")
        val marker = onEdt { Rectangle(window.x + 284, window.y + window.insets.top + 84, 16, 16) }
        val foreground = capture(robot, marker, "$name-foreground")
        val rgb = foreground.getRGB(8, 8)
        assertTrue(((rgb shr 16) and 255) > 230 && (rgb and 255) > 230 && ((rgb shr 8) and 255) < 20,
            "$name foreground must remain sharp and visible above material")
    }

    private fun floatingWindow(): Window? = Window.getWindows().filterIsInstance<JFrame>()
        .firstOrNull { it.title == FLOATING_TITLE && it.isShowing }

    private fun capture(robot: Robot, bounds: Rectangle, name: String): BufferedImage {
        Thread.sleep(400)
        return robot.createScreenCapture(bounds).also {
            val file = File("build/native-backdrop/$name.png").apply { parentFile.mkdirs() }
            ImageIO.write(it, "png", file)
        }
    }

    private fun meanDifference(a: BufferedImage, b: BufferedImage): Double {
        var total = 0L
        for (y in 0 until a.height) for (x in 0 until a.width) for (shift in listOf(0, 8, 16)) {
            total += kotlin.math.abs(((a.getRGB(x, y) shr shift) and 255) - ((b.getRGB(x, y) shr shift) and 255))
        }
        return total.toDouble() / (a.width * a.height * 3)
    }

    private fun await(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(30)
        }
        error("Timed out waiting for $description")
    }

    private fun <T> onEdt(action: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return action()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(action) }
        return checkNotNull(result).getOrThrow()
    }

    private companion object { const val FLOATING_TITLE = "Detached material fixture" }
}
