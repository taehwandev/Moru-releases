package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.component.LocalMoruPopupOwner
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.model.panel.PanelSplit
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import java.awt.Window as AwtWindow

/**
 * Three terminals of one Task in a real window, laid out from a saved tree: the first takes the saved share of the
 * width, and the seam between the two stacked terminals takes a mouse drag over their native views, reporting the
 * resized tree without replacing either terminal.
 */
class TerminalSplitResizeOsTest {
    @Test
    fun savedSharesSizeTheTerminalsAndTheStackedSeamDragsOverThem() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        listOf("one", "two", "three").forEach { title ->
            sessions.createSession(TerminalSessionSpec(title = title, attachedItemId = "task-1", shell = "/bin/sh"))
        }
        val split = mutableStateOf<PanelNode>(PanelLayouts.resize(PanelLayouts.grid(3), emptyList(), 0, 0.7f))
        val reported = AtomicReference<PanelNode?>(null)
        val frame = AtomicReference<AwtWindow?>(null)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-split-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(position = WindowPosition(40.dp, 40.dp))
                state.size = androidx.compose.ui.unit.DpSize(1000.dp, 700.dp)
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal split test") {
                    frame.set(window)
                    MoruTheme {
                        androidx.compose.runtime.CompositionLocalProvider(LocalMoruPopupOwner provides window) {
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
                                split = split.value,
                                onSplitChange = { reported.set(it); split.value = it },
                            )
                        }
                    }
                }
            }
        }
        try {
            val ids = sessions.listSessions().map { it.id }
            fun componentOf(id: String): JComponent? =
                JediTermSessionRegistry.find(id)?.session?.component()?.takeIf { it.isShowing }
            waitFor("all three terminals to show") { ids.takeIf { all -> all.all { componentOf(it) != null } } }
            Thread.sleep(800)
            val content = (frame.get() as RootPaneContainer).contentPane
            val first = componentOf(ids[0])!!
            val second = componentOf(ids[1])!!
            val firstShare = first.width.toDouble() / content.width
            assertTrue(firstShare in 0.62..0.72, "the first terminal must take the saved 70%: ${first.width} of ${content.width}")

            // The seam between the stacked terminals: on the second one's bottom edge, which is where its pane ends
            // and the third pane's header starts.
            val secondOrigin = SwingUtilities.convertPoint(second, 0, 0, content)
            val seamX = secondOrigin.x + second.width / 2
            val seamY = secondOrigin.y + second.height + 1
            val target = SwingUtilities.getDeepestComponentAt(content, seamX, seamY)
            assertTrue(
                target is JComponent && target.cursor.type == Cursor.N_RESIZE_CURSOR,
                "the stacked seam must be a vertical drag target over the terminals: $target",
            )
            val before = second.height
            SwingUtilities.invokeAndWait {
                val point = SwingUtilities.convertPoint(content, seamX, seamY, target)
                fun send(id: Int, dy: Int) = target.dispatchEvent(
                    MouseEvent(
                        target, id, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                        point.x, point.y + dy, seamX + 40, seamY + 40 + dy, 1, false, MouseEvent.BUTTON1,
                    ),
                )
                send(MouseEvent.MOUSE_PRESSED, 0)
                send(MouseEvent.MOUSE_DRAGGED, 60)
                send(MouseEvent.MOUSE_DRAGGED, 120)
            }
            val resized = waitFor("the drag to report a resized tree") { reported.get() as? PanelSplit }
            val column = resized.children[1] as PanelSplit
            assertTrue(column.shares[0] > 0.6f, "dragging down must grow the upper terminal: ${column.shares}")
            assertEquals(0.7f, resized.shares[0], 0.001f)
            waitFor("the upper terminal to grow past $before") { second.height.takeIf { it > before + 60 } }
            assertSame(second, componentOf(ids[1]), "resizing must keep the same terminal view")
        } finally {
            closeWindow.get().invoke()
            app.join(5_000)
            JediTermSessionRegistry.releaseAll()
        }
    }

    @Test
    fun draggingATerminalsHeaderOntoAnotherSwapsTheirPlaces() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        listOf("one", "two").forEach { title ->
            sessions.createSession(TerminalSessionSpec(title = title, attachedItemId = "task-1", shell = "/bin/sh"))
        }
        val split = mutableStateOf<PanelNode?>(null)
        val reported = AtomicReference<PanelNode?>(null)
        val frame = AtomicReference<AwtWindow?>(null)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-swap-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(position = WindowPosition(40.dp, 40.dp))
                state.size = androidx.compose.ui.unit.DpSize(1000.dp, 600.dp)
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal swap test") {
                    frame.set(window)
                    MoruTheme {
                        androidx.compose.runtime.CompositionLocalProvider(LocalMoruPopupOwner provides window) {
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
                                split = split.value,
                                onSplitChange = { reported.set(it); split.value = it },
                            )
                        }
                    }
                }
            }
        }
        try {
            val ids = sessions.listSessions().map { it.id }
            fun componentOf(id: String): JComponent? =
                JediTermSessionRegistry.find(id)?.session?.component()?.takeIf { it.isShowing }
            waitFor("both terminals to show") { ids.takeIf { all -> all.all { componentOf(it) != null } } }
            Thread.sleep(800)
            val content = (frame.get() as RootPaneContainer).contentPane
            val first = componentOf(ids[0])!!
            val second = componentOf(ids[1])!!
            val firstOrigin = SwingUtilities.convertPoint(first, 0, 0, content)
            val secondOrigin = SwingUtilities.convertPoint(second, 0, 0, content)
            assertTrue(firstOrigin.x < secondOrigin.x, "the first terminal starts on the left")

            // Press on the first terminal's header (just above its view) and drop in the middle of the second.
            val from = java.awt.Point(firstOrigin.x + first.width / 3, firstOrigin.y - 12)
            val to = java.awt.Point(secondOrigin.x + second.width / 2, secondOrigin.y + second.height / 2)
            val canvas = SwingUtilities.getDeepestComponentAt(content, from.x, from.y)
            SwingUtilities.invokeAndWait {
                fun send(id: Int, at: java.awt.Point, buttons: Int) {
                    val local = SwingUtilities.convertPoint(content, at, canvas)
                    canvas.dispatchEvent(
                        MouseEvent(canvas, id, System.currentTimeMillis(), buttons, local.x, local.y, 1, false, MouseEvent.BUTTON1),
                    )
                }
                send(MouseEvent.MOUSE_PRESSED, from, InputEvent.BUTTON1_DOWN_MASK)
                (1..10).forEach { step ->
                    val x = from.x + (to.x - from.x) * step / 10
                    val y = from.y + (to.y - from.y) * step / 10
                    send(MouseEvent.MOUSE_DRAGGED, java.awt.Point(x, y), InputEvent.BUTTON1_DOWN_MASK)
                }
                send(MouseEvent.MOUSE_RELEASED, to, 0)
            }
            val swapped = waitFor("the drop to report a swapped tree") { reported.get() }
            assertEquals(listOf(1, 0), PanelLayouts.slots(swapped))
            waitFor("the terminals to trade places") {
                val a = componentOf(ids[0]) ?: return@waitFor null
                val b = componentOf(ids[1]) ?: return@waitFor null
                Unit.takeIf {
                    SwingUtilities.convertPoint(a, 0, 0, content).x > SwingUtilities.convertPoint(b, 0, 0, content).x
                }
            }
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
