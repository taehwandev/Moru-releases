package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.SessionShellProcesses
import app.thdev.moru.core.terminal.host.TerminalHost
import app.thdev.moru.core.terminal.host.TerminalHostLauncher
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The user's case: a terminal is left running, Moru quits and starts again, and the terminal is still there with what
 * it showed. Closing the terminal afterwards ends its program.
 */
class TerminalSurvivesQuittingTest {
    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "timed out waiting for $what" }
            Thread.sleep(50)
        }
    }

    private fun open(key: String, startupCommand: String) = acquireDesktopTerminalSession(
        sessionKey = key,
        workingDirectory = System.getProperty("java.io.tmpdir"),
        shell = "/bin/sh",
        startupCommand = startupCommand,
        onCommandCaptured = {},
        fontFamily = null,
        fontSize = 13,
        scrollbackLines = 200,
        hostKey = "moru-quit-test",
    )

    @Test
    fun aTerminalLeftRunningIsStillThereAfterMoruQuitsAndComesBack() {
        val data = Files.createTempDirectory("moru-quit").toFile()
        // The test's own host, never a detached one: a detached host would outlive the run with its shells.
        val socket = data.resolve("run/q.sock").toPath()
        TerminalHostLauncher.configureSocketForTest(socket)
        thread(isDaemon = true) { TerminalHost(socket).run() }
        waitFor("the test's host to listen") { TerminalHostLauncher.heldKeys() != null }
        try {
            // The prompt is the screen's last line, so it carries the mark: hello-42 only appears once the shell ran it.
            open("term-before", "PS1='hello-\$((6*7)) > '")
            waitFor("the first output") { SessionLastLines.lineFor("term-before")?.contains("hello-42") == true }
            val pid = assertNotNull(SessionShellProcesses.snapshot()["term-before"])

            // Moru quits.
            releaseAllTerminalSessions()
            Thread.sleep(300)
            assertTrue(ProcessHandle.of(pid).map { it.isAlive }.orElse(false), "the shell must outlive Moru")

            // Moru starts again: the same shell, showing what it showed, and the first command is not sent again.
            open("term-after", "echo should-not-run")
            waitFor("the replayed screen") { SessionLastLines.lineFor("term-after")?.contains("hello-42") == true }
            assertEquals(pid, SessionShellProcesses.snapshot()["term-after"])
            Thread.sleep(500)
            assertFalse(SessionLastLines.lineFor("term-after").orEmpty().contains("should-not-run"))

            // The user closes it: now it ends.
            releaseTerminalSession("term-after")
            waitFor("the shell to end") { !ProcessHandle.of(pid).map { it.isAlive }.orElse(false) }
        } finally {
            // Ending every program, not only letting go, so a failed run leaves no shell behind.
            TerminalHostLauncher.terminateAll()
            releaseAllTerminalSessions()
            data.deleteRecursively()
        }
    }
}
