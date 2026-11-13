package app.thdev.moru.core.terminal

import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionShellProcessesTest {
    @Test
    fun aRealShellIsRunningAProgramOnlyWhileOneRuns() {
        val sessionId = "shell-processes-${System.nanoTime()}"
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            startupCommand = "sleep 3",
            onProcessStarted = { pid -> SessionShellProcesses.record(sessionId, pid, expectedShell = "/bin/sh") },
        )
        // The connector reads output on demand, like the terminal widget does.
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) {
                if (connector.read(buffer, 0, buffer.size) < 0) break
            }
        }
        try {
            waitUntil("the program to be seen running") { SessionShellProcesses.hasRunningProgram(sessionId) == true }
            assertFalse(SessionShellProcesses.isIdleShell(sessionId))
            waitUntil("the shell to be back at its prompt") { SessionShellProcesses.hasRunningProgram(sessionId) == false }
            assertTrue(SessionShellProcesses.isIdleShell(sessionId))
        } finally {
            connector.close()
            reader.join(2_000)
            SessionShellProcesses.forget(sessionId)
        }
    }

    @Test
    fun stoppingTheRunningProgramLeavesTheShellAtItsPrompt() {
        val sessionId = "stop-program-${System.nanoTime()}"
        val connector = createDesktopJediTermTtyConnector(
            shell = "/bin/sh",
            workingDirectory = System.getProperty("java.io.tmpdir"),
            startupCommand = "sleep 60",
            onProcessStarted = { pid -> SessionShellProcesses.record(sessionId, pid) },
        )
        val reader = thread(isDaemon = true) {
            val buffer = CharArray(1024)
            while (connector.isConnected) {
                if (connector.read(buffer, 0, buffer.size) < 0) break
            }
        }
        try {
            waitUntil("the program to be seen running") { SessionShellProcesses.hasRunningProgram(sessionId) == true }

            // The shell's own startup can run a short-lived program first; let `sleep` be the one running.
            Thread.sleep(1_000)
            assertTrue(SessionShellProcesses.stopRunningProgram(sessionId))

            waitUntil("the shell to be back at its prompt", timeoutMillis = 5_000) {
                SessionShellProcesses.hasRunningProgram(sessionId) == false
            }
            assertTrue(!SessionShellProcesses.stopRunningProgram(sessionId), "nothing is left to stop")
        } finally {
            connector.close()
            reader.join(2_000)
            SessionShellProcesses.forget(sessionId)
        }
    }

    @Test
    fun anUnknownSessionCannotBeJudged() {
        assertNull(SessionShellProcesses.hasRunningProgram("never-started"))
        assertFalse(SessionShellProcesses.isIdleShell("never-started"))
    }

    @Test
    fun aProgramThatReplacedItsShellCannotAuthorizeAnUpdate() {
        val sessionId = "exec-replaced-${System.nanoTime()}"
        val process = ProcessBuilder("/bin/sh", "-c", "exec /bin/sleep 30").start()
        try {
            SessionShellProcesses.record(sessionId, process.pid(), expectedShell = "/bin/sh")
            waitUntil("exec to replace the shell") {
                process.toHandle().info().command().orElse("").substringAfterLast('/') == "sleep"
            }
            assertFalse(SessionShellProcesses.isIdleShell(sessionId))
        } finally {
            process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS)
            SessionShellProcesses.forget(sessionId)
        }
    }

    @Test
    fun aConfiguredNonShellProgramCannotAuthorizeAnUpdate() {
        val sessionId = "custom-program-${System.nanoTime()}"
        val process = ProcessBuilder("/bin/sleep", "30").start()
        try {
            SessionShellProcesses.record(sessionId, process.pid(), expectedShell = "/bin/sleep")
            assertFalse(SessionShellProcesses.isIdleShell(sessionId))
        } finally {
            process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS)
            SessionShellProcesses.forget(sessionId)
        }
    }

    @Test
    fun aShellThatHasEndedCannotBeJudgedEither() {
        val sessionId = "ended-shell-${System.nanoTime()}"
        val process = ProcessBuilder("/bin/sh", "-c", "exit 0").start()
        SessionShellProcesses.record(sessionId, process.pid())
        process.waitFor(5, TimeUnit.SECONDS)

        assertNull(SessionShellProcesses.hasRunningProgram(sessionId))
        assertEquals(0, process.exitValue())
        SessionShellProcesses.forget(sessionId)
    }

    private fun waitUntil(what: String, timeoutMillis: Long = 10_000, check: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            if (check()) return
            Thread.sleep(50)
        }
        assertTrue(false, "timed out waiting for $what")
    }
}
