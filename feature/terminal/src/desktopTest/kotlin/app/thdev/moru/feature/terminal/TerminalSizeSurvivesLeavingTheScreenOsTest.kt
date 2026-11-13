package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.theme.MoruTheme
import java.awt.Component
import java.awt.Container
import java.awt.GraphicsEnvironment
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A terminal left for another screen and shown again must keep its grid the whole time. Any other size in between
 * reaches the running program as a resize, and a TUI such as Claude Code redraws for it: its footer and input box
 * come back broken.
 */
class TerminalSizeSurvivesLeavingTheScreenOsTest {
    @Test
    fun theGridNeverChangesWhileTheTerminalLeavesAndComesBack() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val shown = mutableStateOf(true)
        val closeWindow = AtomicReference<() -> Unit> {}
        val key = "leave-and-return"
        val app = thread(name = "terminal-leave-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(size = DpSize(900.dp, 600.dp), position = WindowPosition(40.dp, 40.dp))
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal leave test") {
                    MoruTheme {
                        if (shown.value) {
                            RealTerminalContainer(
                                modifier = Modifier.fillMaxSize(),
                                sessionKey = key,
                                workingDirectory = System.getProperty("java.io.tmpdir"),
                                shell = "/bin/sh",
                                startupCommand = null,
                                onCommandCaptured = {},
                                fontFamily = null,
                                fontSize = 13,
                                scrollbackLines = 100,
                            )
                        } else {
                            Box(Modifier.fillMaxSize()) // Another screen.
                        }
                    }
                }
            }
        }
        try {
            val panel = waitFor("the terminal to show") {
                JediTermSessionRegistry.find(key)?.session?.component()?.takeIf { it.isShowing }?.let(::terminalPanelIn)
            }
            Thread.sleep(800)
            val settled = onEdt { panel.terminalSizeFromComponent }
            val grids = Collections.synchronizedList(mutableListOf<String>())
            val listener = object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) {
                    grids += panel.terminalSizeFromComponent?.let { "${it.columns}x${it.rows}" } ?: "none"
                }
            }
            onEdt { panel.addComponentListener(listener) }

            repeat(3) {
                shown.value = false
                Thread.sleep(500)
                shown.value = true
                waitFor("the terminal to show again") { panel.takeIf { it.isShowing } }
                Thread.sleep(800)
            }
            onEdt { panel.removeComponentListener(listener) }

            val expected = "${settled!!.columns}x${settled.rows}"
            assertEquals(
                emptyList(),
                grids.filter { it != expected },
                "the grid settled at $expected but leaving and returning resized it through $grids",
            )
            assertEquals(settled, onEdt { panel.terminalSizeFromComponent })
        } finally {
            closeWindow.get().invoke()
            app.join(5_000)
            JediTermSessionRegistry.releaseAll()
        }
    }

    private fun terminalPanelIn(component: Component): MoruTerminalPanel? {
        if (component is MoruTerminalPanel) return component
        if (component !is Container) return null
        return component.components.firstNotNullOfOrNull(::terminalPanelIn)
    }

    private fun <T> onEdt(action: () -> T): T {
        val result = AtomicReference<T>()
        SwingUtilities.invokeAndWait { result.set(action()) }
        return result.get()
    }

    private fun <T : Any> waitFor(what: String, timeoutMillis: Long = 30_000, read: () -> T?): T {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            read()?.let { return it }
            Thread.sleep(100)
        }
        error("timed out waiting for $what")
    }
}
