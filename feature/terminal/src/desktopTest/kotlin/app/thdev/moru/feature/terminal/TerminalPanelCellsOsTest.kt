package app.thdev.moru.feature.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.core.designsystem.component.LocalMoruPopupOwner
import app.thdev.moru.core.designsystem.component.MoruPanelCell
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.panel.PanelArrangement
import app.thdev.moru.core.model.panel.PanelEdge
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.awt.GraphicsEnvironment
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A panel of another feature (a device) among a Task's two terminals in a real window: it is a cell of the same
 * arrangement, after the terminals, with its own share of the area; taking it away leaves the terminals as they were.
 */
class TerminalPanelCellsOsTest {
    @Test
    fun aPanelAmongTheTerminalsTakesACellAndLeavesWithoutMovingThem() {
        if (GraphicsEnvironment.isHeadless()) return // A window needs a display.

        val sessions = InMemoryTerminalSessionManager()
        listOf("one", "two").forEach { title ->
            sessions.createSession(TerminalSessionSpec(title = title, attachedItemId = "task-1", shell = "/bin/sh"))
        }
        val panelWidth = AtomicInteger(0)
        val panelX = AtomicInteger(-1)
        val mounts = AtomicInteger(0)
        val disposals = AtomicInteger(0)
        val device = MoruPanelCell("device:android:emulator-5554", title = "Pixel 9 Pro Fold") { modifier, header ->
            DisposableEffect(Unit) {
                mounts.incrementAndGet()
                onDispose { disposals.incrementAndGet() }
            }
            Box(
                modifier.onGloballyPositioned {
                    panelWidth.set(it.size.width)
                    panelX.set(it.positionInWindow().x.toInt())
                },
            ) { Box(header) }
        }
        val panels = mutableStateOf(listOf(device))
        val split = mutableStateOf<PanelNode?>(null)
        val reported = AtomicReference<PanelNode?>(null)
        val closeWindow = AtomicReference<() -> Unit> {}
        val app = thread(name = "terminal-panel-cells-test", isDaemon = true) {
            application(exitProcessOnExit = false) {
                closeWindow.set(::exitApplication)
                val state = rememberWindowState(position = WindowPosition(40.dp, 40.dp))
                state.size = DpSize(1200.dp, 700.dp)
                Window(onCloseRequest = ::exitApplication, state = state, title = "terminal panel cells test") {
                    MoruTheme {
                        CompositionLocalProvider(LocalMoruPopupOwner provides window) {
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
                                panels = panels.value,
                            )
                        }
                    }
                }
            }
        }
        try {
            val withDevice = waitFor("the device to take a cell") {
                reported.get()?.takeIf { PanelLayouts.slots(it).size == 3 && panelWidth.get() > 0 }
            }
            assertEquals(listOf(0, 1, 2), PanelLayouts.slots(withDevice).sorted())
            assertTrue(panelWidth.get() > 200, "the device cell must have room of its own: ${panelWidth.get()}")
            assertEquals(1, mounts.get(), "the cell must mount once")
            assertEquals(0, disposals.get(), "the cell must not dispose while shown")
            val initialX = panelX.get()

            // Swap and move the cell across tree depths: its composition must move in place without remounting.
            split.value = PanelLayouts.swap(withDevice, 0, 2)
            waitFor("the swap to move the cell to the left column") { panelX.get().takeIf { it != initialX } }
            split.value = PanelArrangement.move(split.value!!, 2, 0, PanelEdge.Right)
            waitFor("the move to return the cell to the right column") { panelX.get().takeIf { it == initialX } }
            assertEquals(1, mounts.get(), "changing screen position must not remount the cell")
            assertEquals(0, disposals.get(), "changing screen position must not dispose the cell")

            panels.value = emptyList()
            val without = waitFor("the device to leave") { reported.get()?.takeIf { PanelLayouts.slots(it).size == 2 } }
            assertEquals(listOf(0, 1), PanelLayouts.slots(without).sorted())
        } finally {
            closeWindow.get().invoke()
            app.join(5_000)
            JediTermSessionRegistry.releaseAll()
        }
    }

    private fun <T : Any> waitFor(what: String, read: () -> T?): T {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            read()?.let { return it }
            Thread.sleep(50)
        }
        error("Timed out waiting for $what")
    }
}
