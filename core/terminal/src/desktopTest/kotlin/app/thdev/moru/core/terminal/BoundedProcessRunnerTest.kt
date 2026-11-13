package app.thdev.moru.core.terminal

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs only local stand-in commands; process ids come from this test's own children. */
class BoundedProcessRunnerTest {
    @Test
    fun normalCommandPreservesExitStatusAndOutputAtTheExactLimit() {
        val result = assertNotNull(BoundedProcessRunner.run(
            command = listOf("/bin/sh", "-c", "printf 1234567; exit 7"),
            timeoutMillis = 2_000,
            maxOutputBytes = 7,
        ))
        assertEquals(7, result.exitCode)
        assertEquals("1234567", result.text())
    }

    @Test
    fun mergedErrorOutputIsCapturedButImageModeDiscardsIt() {
        val command = listOf("/bin/sh", "-c", "printf image; printf warning >&2")
        assertEquals("imagewarning", BoundedProcessRunner.run(command, 2_000, mergeErrors = true)?.text())
        assertEquals("image", BoundedProcessRunner.run(command, 2_000, mergeErrors = false)?.text())
    }

    @Test
    fun outputOverflowStopsAndReapsTheOwnedProcessWithoutWaitingForTheDeadline() = withProcessFiles { folder, pids ->
        val startedAt = System.nanoTime()
        val result = BoundedProcessRunner.run(
            command = shell("echo \$\$ > '${pids.path}'; while :; do printf 1234567890; done"),
            timeoutMillis = 5_000,
            maxOutputBytes = 1024,
            workingDirectory = folder,
        )
        assertNull(result)
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) < 3_000)
        assertOwnedProcessesStopped(pids)
    }

    @Test
    fun processTimeoutAlsoStopsItsOwnedSleepingChild() = withProcessFiles { _, pids ->
        val result = BoundedProcessRunner.run(
            command = shell("sleep 30 & child=\$!; echo \"\$\$ \$child\" > '${pids.path}'; wait"),
            timeoutMillis = 200,
        )
        assertNull(result)
        assertOwnedProcessesStopped(pids)
    }

    @Test
    fun closedStdoutDoesNotCountAsProcessExitOrAddASecondTimeout() = withProcessFiles { _, pids ->
        val startedAt = System.nanoTime()
        val result = BoundedProcessRunner.run(
            command = shell("sleep 30 & child=\$!; echo \"\$\$ \$child\" > '${pids.path}'; exec 1>&-; exec 2>&-; wait"),
            timeoutMillis = 200,
        )
        assertNull(result)
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) < 1_000)
        assertOwnedProcessesStopped(pids)
    }

    @Test
    fun interruptionReachesTheCallerAndLeavesNoProcessOrReaderTaskBehind() = withProcessFiles { _, pids ->
        val failure = AtomicReference<Throwable?>()
        val runner = thread(name = "bounded-process-interruption-test") {
            try {
                BoundedProcessRunner.run(
                    command = shell("sleep 30 & child=\$!; echo \"\$\$ \$child\" > '${pids.path}'; wait"),
                    timeoutMillis = 30_000,
                )
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        try {
            waitUntil { pids.isFile && pids.readText().trim().isNotBlank() }
            runner.interrupt()
            runner.join(2_000)
            assertFalse(runner.isAlive)
            assertIs<InterruptedException>(failure.get())
            assertOwnedProcessesStopped(pids)
        } finally {
            runner.interrupt()
            runner.join(2_000)
        }
    }

    @Test
    fun alreadyInterruptedCallerDoesNotStartAProcess() = withProcessFiles { _, pids ->
        val failure = AtomicReference<Throwable?>()
        val runner = thread {
            Thread.currentThread().interrupt()
            try {
                BoundedProcessRunner.run(shell("echo started > '${pids.path}'"), 2_000)
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        runner.join(2_000)
        assertFalse(runner.isAlive)
        assertIs<InterruptedException>(failure.get())
        assertFalse(pids.exists())
    }

    private fun shell(script: String) = listOf("/bin/sh", "-c", script)

    private fun assertOwnedProcessesStopped(file: File) {
        val ids = file.readText().trim().split(Regex("\\s+")).map(String::toLong)
        assertTrue(ids.isNotEmpty())
        waitUntil { ids.none { ProcessHandle.of(it).orElse(null)?.isAlive == true } }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(condition(), "The owned process did not reach the expected state.")
    }

    private fun withProcessFiles(block: (File, File) -> Unit) {
        val folder = Files.createTempDirectory("moru-bounded-process").toFile()
        val pids = folder.resolve("owned-pids.txt")
        try {
            block(folder, pids)
        } finally {
            if (pids.isFile) pids.readText().trim().split(Regex("\\s+"))
                .mapNotNull(String::toLongOrNull).forEach { ProcessHandle.of(it).orElse(null)?.destroyForcibly() }
            folder.deleteRecursively()
        }
    }
}
