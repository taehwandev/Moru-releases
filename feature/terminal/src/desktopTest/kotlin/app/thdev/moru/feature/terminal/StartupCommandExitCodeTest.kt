package app.thdev.moru.feature.terminal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Android build terminal ends its command with `exit $?` so the terminal's own exit code is the build's
 * result. This pins that premise against a real shell: the code a command returns is what the session reports.
 */
class StartupCommandExitCodeTest {
    private val keys = mutableListOf<String>()

    @AfterTest
    fun release() = keys.forEach(::releaseTerminalSession)

    private fun exitCodeOf(command: String): Int? {
        val key = "exit-code-test-${System.nanoTime()}".also(keys::add)
        val reported = AtomicReference<Int?>(null)
        val done = CountDownLatch(1)
        acquireDesktopTerminalSession(
            sessionKey = key,
            workingDirectory = System.getProperty("java.io.tmpdir"),
            shell = "/bin/sh",
            startupCommand = command,
            onCommandCaptured = {},
            fontFamily = null,
            fontSize = 13,
            scrollbackLines = 100,
            onExit = { code -> reported.set(code); done.countDown() },
        )
        assertTrue(done.await(20, TimeUnit.SECONDS), "the terminal never exited for: $command")
        return reported.get()
    }

    @Test
    fun aFailingCommandEndsTheTerminalWithItsNonZeroCode() {
        assertEquals(3, exitCodeOf("sh -c 'exit 3'; exit \$?"))
    }

    @Test
    fun aSucceedingCommandEndsTheTerminalWithZero() {
        assertEquals(0, exitCodeOf("true; exit \$?"))
    }
}
