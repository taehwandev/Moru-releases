package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.SessionShellProcesses
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmitLineToTerminalSessionTest {
    private val key = "submit-test-${System.nanoTime()}"

    @AfterTest
    fun release() = releaseTerminalSession(key)

    private fun open(startupCommand: String?) = acquireDesktopTerminalSession(
        sessionKey = key,
        workingDirectory = System.getProperty("java.io.tmpdir"),
        shell = "/bin/sh",
        startupCommand = startupCommand,
        onCommandCaptured = {},
        fontFamily = null,
        fontSize = 13,
        scrollbackLines = 100,
    )

    private fun waitUntil(what: String, timeoutMillis: Long = 15_000, check: () -> Boolean) {
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        while (System.nanoTime() < deadline) {
            if (check()) return
            Thread.sleep(50)
        }
        assertTrue(false, "timed out waiting for $what")
    }

    @Test
    fun aSubmittedLineIsRunByTheShellWithoutPressingEnterByHand() {
        val out = File.createTempFile("submitted", ".txt").apply { deleteOnExit() }
        open(startupCommand = null)

        assertTrue(submitLineToTerminalSession(key, "printf submitted > '${out.absolutePath}'"))

        waitUntil("the line to run") { out.readText() == "submitted" }
    }

    @Test
    fun whenAProgramQuitsTheSameTerminalIsDetectedAtItsPromptAndTakesTheResumeLine() {
        val out = File.createTempFile("resumed", ".txt").apply { deleteOnExit() }
        open(startupCommand = "sleep 2")

        waitUntil("the program to be seen running") { SessionShellProcesses.hasRunningProgram(key) == true }
        waitUntil("the shell to be back at its prompt") { SessionShellProcesses.hasRunningProgram(key) == false }
        assertTrue(submitLineToTerminalSession(key, "printf resumed > '${out.absolutePath}'"))

        waitUntil("the resume line to run in the same terminal") { out.readText() == "resumed" }
        assertEquals(false, SessionShellProcesses.hasRunningProgram(key))
    }

    @Test
    fun anUnknownTerminalCannotBeSubmittedTo() {
        assertFalse(submitLineToTerminalSession("no-such-session", "ls"))
    }
}
