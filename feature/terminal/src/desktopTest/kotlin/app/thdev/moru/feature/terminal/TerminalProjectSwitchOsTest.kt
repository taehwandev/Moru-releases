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
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.SessionShellProcesses
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.test.*

/** Switching Projects in one Compose slot must mount the selected Task's retained native terminal. */
class TerminalProjectSwitchOsTest {
    @Test
    fun switchingProjectsKeepsEachNativeTerminalProcessDirectoryAndScreen() {
        if (GraphicsEnvironment.isHeadless()) return
        val root = Files.createTempDirectory("moru-project-terminals").toFile()
        val alpha = root.resolve("Alpha").apply { mkdir() }
        val moved = alpha.resolve("moved").apply { mkdir() }
        val beta = root.resolve("Beta").apply { mkdir() }
        val alphaKey = "alpha-${UUID.randomUUID()}"
        val betaKey = "beta-${UUID.randomUUID()}"
        val selected = mutableStateOf(alphaKey)
        val closeWindow = AtomicReference<() -> Unit>({})
        val failure = AtomicReference<Throwable?>()
        val app = thread(name = "project-terminal-switch", isDaemon = true) {
            runCatching {
                application(exitProcessOnExit = false) {
                    closeWindow.set(::exitApplication)
                    val state = rememberWindowState(
                        size = DpSize(900.dp, 600.dp), position = WindowPosition((-2200).dp, (-1200).dp),
                    )
                    Window(onCloseRequest = ::exitApplication, state = state,
                        title = "Project terminal switch", focusable = false) {
                        window.isAutoRequestFocus = false
                        MoruTheme {
                            val isAlpha = selected.value == alphaKey
                            RealTerminalContainer(
                                modifier = Modifier.fillMaxSize(), sessionKey = selected.value,
                                workingDirectory = if (isAlpha) alpha.path else beta.path,
                                shell = "/bin/sh",
                                startupCommand = if (isAlpha) "pwd > cwd.txt; PS1='ALPHA_ROOT > '" else "pwd > cwd.txt; PS1='BETA_ROOT > '",
                                onCommandCaptured = {}, fontFamily = null, fontSize = 13,
                                scrollbackLines = 100, focused = false,
                            )
                        }
                    }
                }
            }.onFailure(failure::set)
        }
        fun await(description: String, condition: () -> Boolean) {
            val deadline = System.nanoTime() + 12_000_000_000L
            while (System.nanoTime() < deadline) {
                failure.get()?.let { throw AssertionError("Native window failed", it) }
                if (condition()) return
                Thread.sleep(50)
            }
            error("Timed out waiting for $description; Alpha=${SessionLastLines.lineFor(alphaKey)}; Beta=${SessionLastLines.lineFor(betaKey)}")
        }
        fun showing(key: String) = onEdt {
            JediTermSessionRegistry.find(key)?.session?.component()?.isShowing == true
        }
        try {
            await("Alpha's native terminal") { showing(alphaKey) }
            await("Alpha's shell in its own directory") { SessionLastLines.lineFor(alphaKey)?.contains("ALPHA_ROOT") == true }
            assertEquals(alpha.canonicalPath, alpha.resolve("cwd.txt").readText().trim())
            val originalAlpha = assertNotNull(JediTermSessionRegistry.find(alphaKey))
            val alphaPid = assertNotNull(SessionShellProcesses.snapshot()[alphaKey])
            assertTrue(submitLineToTerminalSession(alphaKey, "cd moved; pwd > cwd.txt; PS1='ALPHA_MOVED > '"))
            await("Alpha's changed directory") { SessionLastLines.lineFor(alphaKey)?.contains("ALPHA_MOVED") == true }
            assertEquals(moved.canonicalPath, moved.resolve("cwd.txt").readText().trim())

            selected.value = betaKey
            await("Beta's native terminal after selecting Beta") { showing(betaKey) }
            assertFalse(showing(alphaKey))
            await("Beta's own directory") { SessionLastLines.lineFor(betaKey)?.contains("BETA_ROOT") == true }
            assertEquals(beta.canonicalPath, beta.resolve("cwd.txt").readText().trim())
            val originalBeta = assertNotNull(JediTermSessionRegistry.find(betaKey))
            val betaPid = assertNotNull(SessionShellProcesses.snapshot()[betaKey])
            assertNotSame(originalAlpha.session.component(), originalBeta.session.component())
            assertTrue(ProcessHandle.of(alphaPid).map { it.isAlive }.orElse(false))

            repeat(2) {
                selected.value = alphaKey
                await("the original Alpha terminal") { showing(alphaKey) }
                assertFalse(showing(betaKey))
                assertSame(originalAlpha, JediTermSessionRegistry.find(alphaKey))
                assertEquals(alphaPid, SessionShellProcesses.snapshot()[alphaKey])
                assertTrue(SessionLastLines.lineFor(alphaKey).orEmpty().contains("ALPHA_MOVED"))
                assertTrue(ProcessHandle.of(betaPid).map { it.isAlive }.orElse(false))
                selected.value = betaKey
                await("the original Beta terminal") { showing(betaKey) }
                assertSame(originalBeta, JediTermSessionRegistry.find(betaKey))
                assertEquals(betaPid, SessionShellProcesses.snapshot()[betaKey])
                assertTrue(SessionLastLines.lineFor(betaKey).orEmpty().contains("BETA_ROOT"))
            }
        } finally {
            closeWindow.get().invoke()
            app.join(5_000)
            onEdt { listOf(alphaKey, betaKey).forEach(JediTermSessionRegistry::release) }
            root.deleteRecursively()
            check(!app.isAlive) { "Native window did not close" }
        }
    }

    private fun <T> onEdt(action: () -> T): T {
        val result = AtomicReference<T>()
        SwingUtilities.invokeAndWait { result.set(action()) }
        return result.get()
    }
}
