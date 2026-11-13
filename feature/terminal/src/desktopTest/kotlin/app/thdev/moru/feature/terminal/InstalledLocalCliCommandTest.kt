package app.thdev.moru.feature.terminal

import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import app.thdev.moru.core.terminal.terminalScreenRows

/** Opt-in, read-only native menu acceptance. Never submits a task, answers trust or selects a ticket. */
class InstalledLocalCliCommandTest {
    @Test fun codexUsage() = verify("codex", "/usage", "exec codex --no-daemon")
    @Test fun codexStatus() = verify("codex", "/status", "exec codex --no-daemon")
    @Test fun claudeUsage() = verifyPrinted("claude", "exec claude --print /usage")
    @Test fun claudeStatus() = verify("claude", "/status", "exec claude")
    @Test fun agyUsage() = verifyPrinted("agy", "exec agy -p /usage")

    private fun verifyPrinted(provider: String, startup: String) {
        assumeTrue(System.getenv("MORU_VERIFY_LOCAL_CLI") == "1")
        val key = "native-cli-check-${UUID.randomUUID()}"
        val exit = AtomicReference<Int?>()
        val ended = CountDownLatch(1)
        try {
            acquireDesktopTerminalSession(
                sessionKey = key, workingDirectory = System.getenv("MORU_LOCAL_CLI_FOLDER") ?: System.getProperty("user.dir"), shell = "/bin/zsh",
                startupCommand = startup, onCommandCaptured = {}, fontFamily = null, fontSize = 13,
                scrollbackLines = 100, onOutput = {}, onExit = { exit.set(it); ended.countDown() },
            )
            assertTrue(ended.await(30, TimeUnit.SECONDS), "$provider native usage did not finish")
            assertEquals(0, exit.get(), "$provider native usage failed")
            val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            var result = false
            while (!result && System.nanoTime() < until) {
                val text = screenRows(key).joinToString("\n")
                result = (text.contains("%") || text.contains("quota", true)) && (text.contains("reset", true) || text.contains("remaining", true))
                if (!result) Thread.sleep(25)
            }
            assertTrue(result, "$provider native usage did not print quota/reset data")
        } finally { releaseTerminalSession(key) }
    }

    private fun verify(provider: String, command: String, startup: String) = runBlocking {
        assumeTrue(System.getenv("MORU_VERIFY_LOCAL_CLI") == "1")
        val key = "native-cli-check-${UUID.randomUUID()}"
        try {
            acquireDesktopTerminalSession(
                sessionKey = key, workingDirectory = System.getenv("MORU_LOCAL_CLI_FOLDER") ?: System.getProperty("user.dir"), shell = "/bin/zsh",
                startupCommand = startup, onCommandCaptured = {}, fontFamily = null, fontSize = 13,
                scrollbackLines = 100, onOutput = {}, onExit = {},
            )
            val error = executeLocalCliCommand(key, provider, command)
            if (error != null) {
                val rows = screenRows(key)
                // Only fixed diagnostic flags leave this transient screen, never account identity or paths.
                println("native $provider $command: composer=" + rows.any { it.trim().startsWith("›") || it.trim().startsWith("❯") || it.trim().startsWith(">") } +
                    " echoed=" + rows.any { it.contains(command) } + " usage=" + rows.any { it.contains("usage", true) } +
                    " trust=" + rows.any { it.contains("trust", true) } + " login=" + rows.any { it.contains("login", true) })
            }
            assertNull(error, "$provider $command did not produce its native result")
        } finally { releaseTerminalSession(key) }
    }

    private fun screenRows(key: String): List<String> {
        val buffer = JediTermSessionRegistry.find(key)?.session?.textBuffer ?: return emptyList()
        buffer.lock()
        return try { terminalScreenRows(buffer) } finally { buffer.unlock() }
    }
}
