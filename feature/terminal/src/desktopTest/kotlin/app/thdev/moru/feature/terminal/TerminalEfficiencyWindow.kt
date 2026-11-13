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
import app.thdev.moru.core.terminal.DesktopPtyTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/** An isolated native pane; only its own registry keys and temporary directory are released. */
internal class TerminalEfficiencyWindow : AutoCloseable {
    private val folder = Files.createTempDirectory("moru-efficiency").toFile()
    private val workspaceId = "efficiency-${UUID.randomUUID()}"
    private val delegate = DesktopPtyTerminalSessionManager(spawnProcesses = false)
    val revisionReads = AtomicLong()
    val manager = object : TerminalSessionManager by delegate {
        override fun revision(): Long {
            revisionReads.incrementAndGet()
            return delegate.revision()
        }
    }
    private val keys = mutableListOf<String>()
    private val redraw = mutableStateOf(0)
    private val closeWindow = AtomicReference<() -> Unit>({})
    private val failure = AtomicReference<Throwable?>()
    val first = addSession()
    private val app = thread(name = "terminal-efficiency-fixture", isDaemon = true) {
        runCatching {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(
                    size = DpSize(1000.dp, 500.dp), position = WindowPosition((-2200).dp, (-1200).dp),
                )
                Window(
                    onCloseRequest = ::exitApplication, state = state,
                    title = "Terminal efficiency fixture", focusable = false,
                ) {
                    window.isAutoRequestFocus = false
                    MoruTheme {
                        MultiTerminalPane(
                            sessionManager = manager, defaultWorkingDirectory = folder.absolutePath,
                            defaultShell = "/bin/sh", workspaceId = workspaceId,
                            workspaceName = "Efficiency fixture ${redraw.value}",
                            terminalFontFamily = null, terminalFontSize = 13,
                            terminalScrollbackLines = 100, modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }.onFailure(failure::set)
    }

    fun addSession(): String = manager.createSession(
        TerminalSessionSpec(
            title = "Fixture ${keys.size + 1}", attachedItemId = workspaceId,
            shell = "/bin/sh", workingDirectory = folder.absolutePath,
            startupCommand = "printf 'efficiency fixture\\n'; exec /bin/sleep 120",
        ),
    ).id.also(keys::add)

    fun waitUntil(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            failure.get()?.let { throw AssertionError("Native fixture failed", it) }
            if (condition()) return
            Thread.sleep(50)
        }
        error("Timed out waiting for $description")
    }

    fun waitForSession(id: String) = waitUntil("visible session $id") { isShowing(id) }

    fun isShowing(id: String): Boolean {
        var showing = false
        SwingUtilities.invokeAndWait { showing = JediTermSessionRegistry.find(id)?.session?.component()?.isShowing == true }
        return showing
    }

    fun keyboardRequests(id: String): Int = JediTermSessionRegistry.find(id)?.session?.keyboardRequests?.get() ?: 0

    fun redrawMetadata() { redraw.value += 1 }

    fun liveSessions(): Int = keys.count { JediTermSessionRegistry.find(it) != null }

    fun visibleSessions(): Int = keys.count(::isShowing)

    override fun close() {
        closeWindow.get().invoke()
        app.join(5_000)
        SwingUtilities.invokeAndWait { keys.forEach(JediTermSessionRegistry::release) }
        keys.forEach(manager::closeSession)
        folder.deleteRecursively()
        check(!app.isAlive) { "Native fixture did not close" }
    }
}
