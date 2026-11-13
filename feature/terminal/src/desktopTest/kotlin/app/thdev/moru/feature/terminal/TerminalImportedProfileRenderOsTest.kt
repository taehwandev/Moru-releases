package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.model.terminal.TerminalProfile
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * A running terminal takes an imported profile's colors and gives them back when the profile is removed, without
 * restarting its program: the same component stays on screen throughout.
 */
class TerminalImportedProfileRenderOsTest {
    @Test
    fun aRunningTerminalRepaintsInTheImportedColorsAndBack() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val profile = mutableStateOf<TerminalProfile?>(null)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-profile-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(size = DpSize(640.dp, 360.dp), position = WindowPosition(40.dp, 40.dp))
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal profile test") {
                    MoruTheme(colors = darkCkColors) {
                        CompositionLocalProvider(LocalTerminalProfile provides profile.value) {
                            RealTerminalContainer(
                                modifier = Modifier.fillMaxSize(),
                                sessionKey = SessionKey,
                                workingDirectory = System.getProperty("java.io.tmpdir"),
                                shell = "/bin/sh",
                                startupCommand = "printf '\\033[31mred \\033[32mgreen \\033[34mblue\\033[0m plain\\n'",
                                onCommandCaptured = {},
                                fontFamily = null,
                                fontSize = 14,
                                scrollbackLines = 100,
                            )
                        }
                    }
                }
            }
        }
        try {
            val component = waitFor("the terminal to show") {
                JediTermSessionRegistry.find(SessionKey)?.session?.component()?.takeIf { it.isShowing }
            }
            Thread.sleep(800)
            assertEquals(TerminalPalette.from(darkCkColors).canvas.rgb(), component.cornerColor("moru-before"))

            profile.value = Solarized
            waitFor("the imported background") { component.cornerColor("imported").takeIf { it == 0x002b36 } }
            assertSame(component, JediTermSessionRegistry.find(SessionKey)?.session?.component())

            profile.value = null
            waitFor("Moru's background again") {
                component.cornerColor("moru-after").takeIf { it == TerminalPalette.from(darkCkColors).canvas.rgb() }
            }
            assertSame(component, JediTermSessionRegistry.find(SessionKey)?.session?.component())
        } finally {
            closeWindow.get().invoke()
            app.join(5_000)
            JediTermSessionRegistry.releaseAll()
        }
    }

    /** Paints the terminal and returns the RGB of its empty bottom-right corner; the image is kept for review. */
    private fun JComponent.cornerColor(stage: String): Int {
        val image = AtomicReference<BufferedImage>()
        SwingUtilities.invokeAndWait {
            image.set(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { paint(it.createGraphics()) })
        }
        val painted = image.get()
        File("build/ui-captures/terminal-profile-$stage.png").apply { parentFile.mkdirs() }.also { ImageIO.write(painted, "png", it) }
        return painted.getRGB(painted.width - 20, painted.height - 20) and 0xffffff
    }

    private fun com.jediterm.core.Color.rgb(): Int = (red shl 16) or (green shl 8) or blue

    private fun <T : Any> waitFor(what: String, timeoutMillis: Long = 30_000, read: () -> T?): T {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            read()?.let { return it }
            Thread.sleep(100)
        }
        error("timed out waiting for $what")
    }

    private companion object {
        const val SessionKey = "terminal-profile-render"
        val Solarized = TerminalProfile(
            source = "iTerm2 · Default",
            dark = TerminalColorScheme(
                foreground = "#839496",
                background = "#002b36",
                selection = "#073642",
                ansi = listOf(
                    "#073642", "#dc322f", "#859900", "#b58900", "#268bd2", "#d33682", "#2aa198", "#eee8d5",
                    "#002b36", "#cb4b16", "#586e75", "#657b83", "#839496", "#6c71c4", "#93a1a1", "#fdf6e3",
                ),
            ),
            lineSpacing = 1.0f,
            cursorBlink = false,
        )
    }
}
