package app.thdev.moru.feature.workbench.resume

import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeRestartGuardProcessTest {
    @Test
    fun resumeCanProceedBeforeTheUiClockHasObservedTheAgentExit() {
        val shell = ProcessBuilder("/bin/sh", "-c", "read line").start()
        val id = "theme-restart-${System.nanoTime()}"
        try {
            SessionShellProcesses.record(id, shell.pid())
            assertNull(SessionShellProcesses.observedRunningProgram(id))
            assertEquals(false, SessionShellProcesses.hasRunningProgram(id))
            // Exiting an agent redraws its terminal. A cached unknown/running reading would classify this as work.
            val snapshot = TerminalSessionSnapshot(
                id, TerminalSessionSpec(title = "Codex", attachedItemId = "task", startupCommand = "codex resume c-1"),
                TerminalSessionStatus.READY, lastOutputAtMillis = 10_000L,
            )
            assertTrue(themeRestartGuard(snapshot, "task", id, 10_001L, isStale = { true }))
        } finally {
            SessionShellProcesses.forget(id)
            shell.outputStream.close()
            shell.destroy()
            if (!shell.waitFor(2, TimeUnit.SECONDS)) shell.destroyForcibly()
        }
    }
}
