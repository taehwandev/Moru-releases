package app.thdev.moru.core.terminal

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Opt-in ownership check on a real native CLI; no task, auth change or menu selection. */
class LocalCliCaptureTest {
    @Test fun cancelledStatusCaptureEndsItsOwnedProcessTree() = runBlocking {
        assumeTrue(System.getenv("MORU_VERIFY_LOCAL_CLI") == "1")
        val current = ProcessHandle.current()
        val before = current.descendants().use { children -> children.map { it.pid() }.toList().toSet() }
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(250) {
                captureClaudeCliStatus(System.getenv("MORU_LOCAL_CLI_FOLDER") ?: System.getProperty("user.dir"), "/bin/zsh")
            }
        }
        val remaining = current.descendants().use { children -> children.filter { it.pid() !in before && it.isAlive }.toList() }
        assertTrue(remaining.isEmpty(), "A cancelled status read must not retain a CLI descendant")
    }
}
