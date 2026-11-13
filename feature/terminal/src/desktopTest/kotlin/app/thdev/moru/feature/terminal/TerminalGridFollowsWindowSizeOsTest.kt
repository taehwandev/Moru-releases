package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Several terminals of one Task, next to a panel, in a real window that is made larger after the terminals were
 * added and one of them selected. Every terminal must stay inside its own half and a click in the panel half must
 * never land on a terminal; a Swing terminal that kept stale bounds or a stale parent leaves a dead strip beside it.
 */
class TerminalGridFollowsWindowSizeOsTest {
    @Test
    fun everyTerminalStaysInItsHalfAndTheClicksBesideThemMissIt() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        val size = mutableStateOf(DpSize(640.dp, 420.dp))
        val frame = AtomicReference<AwtWindow?>(null)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-grid-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(size = size.value, position = WindowPosition(40.dp, 40.dp))
                state.size = size.value
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal grid test") {
                    frame.set(window)
                    MoruTheme {
                        Row(Modifier.fillMaxSize()) {
                            MultiTerminalPane(
                                sessionManager = sessions,
                                defaultWorkingDirectory = System.getProperty("java.io.tmpdir"),
                                defaultShell = "/bin/sh",
                                workspaceId = "task-1",
                                workspaceName = "Task 1",
                                terminalFontFamily = null,
                                terminalFontSize = 13,
                                terminalScrollbackLines = 100,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            // Stands in for the Result pane beside the terminals.
                            Box(Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }
        try {
            fun componentOf(id: String): JComponent? = JediTermSessionRegistry.find(id)?.session?.component()?.takeIf { it.isShowing }
            val first = waitFor("the first terminal to show") {
                sessions.listSessions().firstOrNull()?.id?.takeIf { componentOf(it) != null }
            }
            // A second terminal of the same Task, selected, as an agent launch or a build would add.
            val second = sessions.createSession(TerminalSessionSpec(title = "second", attachedItemId = "task-1", shell = "/bin/sh")).id
            sessions.selectSession(second)
            waitFor("the second terminal to show") { second.takeIf { componentOf(it) != null } }
            // And a third, then go back to the first: the grid is rearranged each time.
            val third = sessions.createSession(TerminalSessionSpec(title = "third", attachedItemId = "task-1", shell = "/bin/sh")).id
            sessions.selectSession(third)
            waitFor("the third terminal to show") { third.takeIf { componentOf(it) != null } }
            sessions.selectSession(first)
            Thread.sleep(800)

            val content = (frame.get() as RootPaneContainer).contentPane
            val ids = listOf(first, second, third)
            fun assertLayout(stage: String) {
                val y = content.height / 2
                ids.forEach { id ->
                    val component = componentOf(id) ?: error("$stage: terminal $id is not showing")
                    val origin = SwingUtilities.convertPoint(component, 0, 0, content)
                    assertTrue(
                        origin.x >= -2 && origin.x + component.width <= content.width / 2 + 8,
                        "$stage: terminal $id sticks out of the left half: x=${origin.x} w=${component.width} content=${content.width}",
                    )
                    val centre = SwingUtilities.getDeepestComponentAt(content, origin.x + component.width / 2, origin.y + component.height / 2)
                    assertTrue(
                        centre != null && SwingUtilities.isDescendingFrom(centre, component),
                        "$stage: the centre of terminal $id does not hit it: $centre (bounds ${component.bounds} at $origin)",
                    )
                }
                for (fraction in listOf(0.55, 0.7, 0.9)) {
                    val hit = SwingUtilities.getDeepestComponentAt(content, (content.width * fraction).toInt(), y)
                    assertTrue(
                        hit == null || ids.none { id -> componentOf(id)?.let { SwingUtilities.isDescendingFrom(hit, it) } == true },
                        "$stage: a click at ${(fraction * 100).toInt()}% of the width, beside the terminals, lands on a terminal: $hit",
                    )
                }
            }
            assertLayout("small window")

            val before = content.width
            size.value = DpSize(1100.dp, 760.dp)
            waitFor("the window to grow past $before (now ${content.width})") { content.width.takeIf { it > before + 200 } }
            Thread.sleep(800)
            assertLayout("large window")
            sessions.selectSession(second)
            Thread.sleep(800)
            assertLayout("large window after selecting another terminal")
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
