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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Two terminals side by side. The screen is redrawn every couple of seconds while agents run, and a redraw must never
 * ask to move the keyboard: a Korean syllable being composed is broken off when focus leaves, and the next keys land
 * in the other terminal. The chosen terminal asks on selection changes and explicit reselection. The asks are counted rather
 * than focus observed, because a test window behind another app never holds the keyboard.
 */
class TerminalKeepsFocusOsTest {
    @Test
    fun explicitReselectionReturnsTheKeyboardButRedrawsNeverTakeIt() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        fun createTerminal(title: String) = sessions.createSession(TerminalSessionSpec(
            title = title, attachedItemId = "task-1", shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
        )).id
        // Creating a session selects it. Prepare both before mounting so the fixture never briefly
        // selects the second one while expecting it to have received no focus requests.
        val first = createTerminal("first")
        val second = createTerminal("second")
        sessions.selectSession(first)
        val redraw = mutableStateOf(0)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-focus-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(size = DpSize(1000.dp, 500.dp), position = WindowPosition(40.dp, 40.dp))
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal focus test") {
                    MoruTheme {
                        MultiTerminalPane(
                            sessionManager = sessions,
                            defaultWorkingDirectory = System.getProperty("java.io.tmpdir"),
                            defaultShell = "/bin/sh",
                            workspaceId = "task-1",
                            // A new name each time stands in for the periodic redraw of the running app.
                            workspaceName = "Task 1 · ${redraw.value}",
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
            waitFor("the first terminal to show") { first.takeIf { componentOf(it) != null } }
            waitFor("the second terminal to show") { second.takeIf { componentOf(it) != null } }
            Thread.sleep(800)

            val firstSession = JediTermSessionRegistry.find(first)!!.session
            val secondSession = JediTermSessionRegistry.find(second)!!.session
            // Only the chosen terminal ever asks for the keyboard; the one beside it never does.
            assertTrue(firstSession.keyboardRequests.get() >= 1, "the chosen terminal never asked for the keyboard")
            assertEquals(0, secondSession.keyboardRequests.get(), "the terminal beside the chosen one asked for the keyboard")
            val askedBefore = firstSession.keyboardRequests.get()

            repeat(5) { round ->
                redraw.value = round + 1
                Thread.sleep(300)
            }

            // A redraw asks on nobody's behalf, so the keyboard stays where the user is typing.
            assertEquals(askedBefore, firstSession.keyboardRequests.get(), "a redraw asked for the keyboard for the chosen terminal")
            assertEquals(0, secondSession.keyboardRequests.get(), "a redraw asked for the keyboard for the other terminal")

            // The sidebar and terminal tabs may select the already-chosen session. Selection stays true,
            // but each deliberate choice must request the keyboard again after it moved to another control.
            repeat(2) {
                val before = firstSession.keyboardRequests.get()
                sessions.selectSession(first)
                requestTerminalFocus(first)
                waitFor("reselection to return the keyboard") {
                    true.takeIf { firstSession.keyboardRequests.get() > before }
                }
                assertEquals(first, sessions.selectedSessionId())
                assertEquals(0, secondSession.keyboardRequests.get(), "reselection requested the other terminal's keyboard")
            }

            val firstAfterReselection = firstSession.keyboardRequests.get()
            // A request for a terminal that is not selected cannot redirect input.
            requestTerminalFocus(second)
            redraw.value += 1
            Thread.sleep(500)
            assertEquals(firstAfterReselection, firstSession.keyboardRequests.get(), "another terminal's request refocused the chosen one")
            assertEquals(0, secondSession.keyboardRequests.get(), "an unselected terminal took the keyboard")

            sessions.selectSession(second)
            requestTerminalFocus(second)
            waitFor("the second terminal to receive its selection") {
                true.takeIf { secondSession.keyboardRequests.get() > 0 }
            }
            Thread.sleep(300)
            val secondAfterSelection = secondSession.keyboardRequests.get()
            repeat(3) {
                redraw.value += 1
                Thread.sleep(300)
            }
            assertEquals(firstAfterReselection, firstSession.keyboardRequests.get(), "switching sessions refocused the previous terminal")
            assertEquals(secondAfterSelection, secondSession.keyboardRequests.get(), "a redraw refocused the newly selected terminal")
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
