package app.thdev.moru.core.terminal

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class SessionThemeRestartsTest {
    @Test
    fun anUnknownShellFailsWithoutSubmittingAndCanRetry() {
        val id = "unknown-${UUID.randomUUID()}"
        val submitted = CopyOnWriteArrayList<String>()
        try {
            repeat(2) {
                assertTrue(SessionThemeRestarts.request(id, "codex", "resume", { true }, { _, line -> submitted += line; true }))
                await { SessionThemeRestarts.state(id) == SessionThemeRestarts.State.Failed }
            }
            assertTrue(submitted.isEmpty())
        } finally {
            SessionThemeRestarts.forget(id)
        }
    }

    @Test
    fun aReplacementProgramIsNeitherStoppedNorGivenAnAgentCommand() = withShell { id, shell ->
        val program = startSleep(shell)
        assertNotNull(SessionShellProcesses.shellProcess(id), "Configured /bin/sh; actual ${shell.toHandle().info().command()}")
        val submitted = CopyOnWriteArrayList<String>()
        assertTrue(SessionThemeRestarts.request(id, "codex", "codex resume own", { true }, { _, line -> submitted += line; true }))
        await { SessionThemeRestarts.state(id) == SessionThemeRestarts.State.Failed }
        assertTrue(program.isAlive)
        assertTrue(submitted.isEmpty())
    }

    @Test
    fun aChangedGuardPreventsStoppingTheOriginalProgram() = withShell { id, shell ->
        val program = startSleep(shell)
        var checks = 0
        assertFalse(restartSessionAgent(id, "sleep", "resume", { ++checks == 1 }, { _, _ -> error("must not submit") }))
        assertEquals(2, checks)
        assertTrue(program.isAlive)
    }

    @Test
    fun execReplacingTheShellNeverReceivesAResumeCommand() = withShell { id, shell ->
        shell.outputStream.write("exec /bin/sleep 30\n".toByteArray())
        shell.outputStream.flush()
        await { shell.toHandle().info().command().orElse("").endsWith("/sleep") }
        assertEquals(false, SessionShellProcesses.hasRunningProgram(id))
        assertFalse(restartSessionAgent(id, "codex", "resume", { true }, { _, _ -> error("must not submit") }))
        assertTrue(shell.isAlive)
    }

    @Test
    fun aChildStartedAtTheFinalGuardNeverReceivesTheResumeLine() = withShell { id, shell ->
        var checks = 0
        assertFalse(restartSessionAgent(id, "codex", "resume", {
            checks++
            if (checks == 2) startSleep(shell)
            true
        }, { _, _ -> error("must not submit") }))
        assertEquals(2, checks)
        assertTrue(shell.children().use { it.anyMatch { child -> child.isAlive } })
    }

    @Test
    fun aSessionOwnedRestartWaitsForTheAgentAndANewColorReadWithoutAView() = withShell { id, shell ->
        val agent = startSleep(shell)
        assertNotNull(SessionShellProcesses.shellProcess(id), "Configured /bin/sh; actual ${shell.toHandle().info().command()}")
        SessionThemeColors.recordCanvas(id, 0x101010)
        SessionThemeColors.recordBackgroundColor(id, 0x101010)
        SessionThemeColors.recordCanvas(id, 0xf0f0f0)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val submitted = CopyOnWriteArrayList<Pair<String, String>>()
        assertTrue(SessionThemeRestarts.request(id, "sleep", "codex resume own", {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
        }, { key, line ->
            assertFalse(agent.isAlive)
            assertEquals(false, SessionShellProcesses.hasRunningProgram(id))
            submitted += key to line
            true
        }))
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        assertFalse(SessionThemeRestarts.request(id, "sleep", "duplicate", { true }, { _, _ -> error("duplicate") }))
        release.countDown()
        await { submitted.isNotEmpty() }
        assertEquals(listOf(id to "codex resume own"), submitted.toList())
        assertTrue(SessionThemeColors.hasStaleBackground(id), "Submitting alone cannot confirm theme application")
        assertEquals(SessionThemeRestarts.State.Running, SessionThemeRestarts.state(id))
        SessionThemeColors.recordBackgroundColor(id, 0xf0f0f0)
        await { SessionThemeRestarts.state(id) == SessionThemeRestarts.State.Applied }
    }

    private fun withShell(check: (String, Process) -> Unit) {
        val id = "restart-${UUID.randomUUID()}"
        val shell = ProcessBuilder("/bin/sh").start()
        SessionShellProcesses.record(id, shell.pid(), "/bin/sh")
        try {
            await { SessionShellProcesses.shellProcess(id) != null }
            check(id, shell)
        } finally {
            shell.descendants().use { it.forEach { child -> child.destroyForcibly() } }
            shell.destroyForcibly()
            shell.waitFor(5, TimeUnit.SECONDS)
            SessionShellProcesses.forget(id)
            SessionThemeColors.forget(id)
        }
    }

    private fun startSleep(shell: Process): ProcessHandle {
        val previous = shell.children().use { it.map(ProcessHandle::pid).toList().toSet() }
        shell.outputStream.write("/bin/sleep 30 &\n".toByteArray())
        shell.outputStream.flush()
        await { shell.children().use { it.anyMatch { child -> child.pid() !in previous } } }
        return shell.children().use { it.filter { child -> child.pid() !in previous }.findFirst().orElseThrow() }
    }

    private fun await(read: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!read() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(read(), "session restart did not reach the expected state")
    }
}
