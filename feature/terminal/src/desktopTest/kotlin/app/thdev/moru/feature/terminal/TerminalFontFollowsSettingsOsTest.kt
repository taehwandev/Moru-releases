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
import java.awt.GraphicsEnvironment
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A terminal already open takes a font size changed in Settings: it draws bigger and fits fewer columns, and it is
 * the same terminal, so the program in it keeps running instead of being reopened.
 */
class TerminalFontFollowsSettingsOsTest {
    @Test
    fun anOpenTerminalTakesTheNewFontSizeWithoutBeingReopened() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        val fontSize = mutableStateOf(13)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-font-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(size = DpSize(720.dp, 420.dp), position = WindowPosition(40.dp, 40.dp))
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal font test") {
                    MoruTheme {
                        MultiTerminalPane(
                            sessionManager = sessions,
                            defaultWorkingDirectory = System.getProperty("java.io.tmpdir"),
                            defaultShell = "/bin/sh",
                            workspaceId = "task-1",
                            workspaceName = "Task 1",
                            terminalFontFamily = null,
                            terminalFontSize = fontSize.value,
                            terminalScrollbackLines = 100,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        try {
            val session = waitFor("the terminal to show") {
                sessions.listSessions().firstOrNull()?.id
                    ?.let { id -> JediTermSessionRegistry.find(id)?.session }
                    ?.takeIf { it.component().isShowing && it.columns > 0 }
            }
            waitFor("the terminal to fit its pane") { session.takeIf { it.columns < 120 } }
            val smallColumns = session.columns
            assertTrue(session.fontSize == 13)

            fontSize.value = 20

            waitFor("the open terminal to take the new size") { session.takeIf { it.fontSize == 20 } }
            waitFor("fewer columns at the bigger size") { session.takeIf { it.columns < smallColumns } }
            val id = sessions.listSessions().single().id
            assertSame(session, JediTermSessionRegistry.find(id)?.session, "The terminal must not be reopened")
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
