package app.thdev.moru.feature.terminal

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
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.awt.GraphicsEnvironment
import java.awt.Window as AwtWindow
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.RootPaneContainer
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Two terminals side by side across the whole window, which is then made larger. Each terminal must grow with its
 * half; a terminal that keeps its old, narrower bounds leaves text wrapping early and a dead strip beside it that
 * swallows clicks.
 */
class TerminalFillsItsPaneOsTest {
    @Test
    fun eachTerminalGrowsToFillItsHalfWhenTheWindowIsMadeLarger() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        val size = mutableStateOf(DpSize(700.dp, 420.dp))
        val frame = AtomicReference<AwtWindow?>(null)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-fill-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(size = size.value, position = WindowPosition(40.dp, 40.dp))
                state.size = size.value
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal fill test") {
                    frame.set(window)
                    MoruTheme {
                        MultiTerminalPane(
                            sessionManager = sessions,
                            defaultWorkingDirectory = System.getProperty("java.io.tmpdir"),
                            defaultShell = "/bin/sh",
                            workspaceId = "task-1",
                            workspaceName = "Task 1",
                            terminalFontFamily = null,
                            terminalFontSize = 13,
                            terminalScrollbackLines = 100,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        try {
            fun componentOf(id: String): JComponent? = JediTermSessionRegistry.find(id)?.session?.component()?.takeIf { it.isShowing }
            val first = waitFor("the first terminal to show") {
                sessions.listSessions().firstOrNull()?.id?.takeIf { componentOf(it) != null }
            }
            val second = sessions.createSession(TerminalSessionSpec(title = "second", attachedItemId = "task-1", shell = "/bin/sh")).id
            sessions.selectSession(second)
            waitFor("the second terminal to show") { second.takeIf { componentOf(it) != null } }
            Thread.sleep(800)

            val content = (frame.get() as RootPaneContainer).contentPane
            fun assertFillsHalf(stage: String) {
                listOf(first, second).forEach { id ->
                    val component = componentOf(id) ?: error("$stage: terminal $id is not showing")
                    assertTrue(
                        component.width >= content.width * 0.42,
                        "$stage: terminal $id is only ${component.width}px wide in a ${content.width}px window; each half is about ${content.width / 2}px",
                    )
                }
            }
            assertFillsHalf("small window")

            val before = content.width
            size.value = DpSize(1400.dp, 800.dp)
            waitFor("the window to grow past $before (now ${content.width})") { content.width.takeIf { it > before + 300 } }
            Thread.sleep(800)
            assertFillsHalf("large window")
        } finally {
            closeWindow.get().invoke()
            app.join(5_000)
            JediTermSessionRegistry.releaseAll()
        }
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
