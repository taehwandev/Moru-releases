package app.thdev.moru.feature.terminal

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Native slash input reaches a live CLI, never a positional AI prompt. Uses no installed provider account. */
class LocalAgentCliPtyTest {
    private val keys = mutableListOf<String>()
    private val files = mutableListOf<File>()

    @AfterTest
    fun release() {
        keys.forEach(::releaseTerminalSession)
        files.forEach { it.deleteRecursively() }
    }

    @Test
    fun nativeSlashIsSubmittedOnlyToTheChosenCliAndItsExitIsReported() {
        val root = kotlin.io.path.createTempDirectory("lab-local-cli").toFile().also(files::add)
        val received = root.resolve("input")
        val ready = root.resolve("ready")
        val cli = root.resolve("cli.sh").apply {
            writeText("""
                printf ready > '${ready.absolutePath}'
                printf 'Native CLI ready\n'
                IFS= read -r line
                printf '%s' "${'$'}line" > '${received.absolutePath}'
                printf 'Local menu: %s\n' "${'$'}line"
                exit 7
            """.trimIndent())
        }
        val key = "lab-cli-pty-${System.nanoTime()}".also(keys::add)
        val code = AtomicReference<Int?>()
        val outputChunks = AtomicInteger()
        val ended = CountDownLatch(1)
        acquireDesktopTerminalSession(
            sessionKey = key, workingDirectory = root.path, shell = "/bin/sh",
            startupCommand = "exec sh '${cli.absolutePath}'", onCommandCaptured = {},
            fontFamily = null, fontSize = 13, scrollbackLines = 100,
            onOutput = { outputChunks.incrementAndGet() },
            onExit = { code.set(it); ended.countDown() },
        )
        waitUntil { ready.exists() }
        assertFalse(received.exists(), "Opening the CLI must not submit a model prompt or ticket action")
        assertTrue(submitLineToTerminalSession(key, "/usage"))
        assertTrue(ended.await(15, TimeUnit.SECONDS))
        assertEquals("/usage", received.readText())
        assertEquals(7, code.get())
        assertTrue(outputChunks.get() > 0, "Native CLI output must reach the terminal")
        releaseTerminalSession(key)
        assertFalse(submitLineToTerminalSession(key, "/logout"), "Released panel must no longer accept input")
    }

    private fun waitUntil(check: () -> Boolean) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < until) {
            if (check()) return
            Thread.sleep(25)
        }
        assertTrue(check(), "The fake CLI did not become ready")
    }
}
