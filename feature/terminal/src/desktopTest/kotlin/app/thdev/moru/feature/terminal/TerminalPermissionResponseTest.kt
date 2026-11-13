package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.core.terminal.SessionQuestions
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalPermissionResponseTest {
    private val key = "permission-pty-${System.nanoTime()}"
    @AfterTest fun release() = releaseTerminalSession(key)

    @Test fun actualPtyAcceptsOneAnswerAndClearsOnClose() {
        val out = File.createTempFile("permission-response", ".txt").apply { deleteOnExit() }
        open("printf 'Allow execution? (y/n)'; read answer; printf '%s' \"\$answer\" > '${out.absolutePath}'")
        waitUntil { SessionPermissions.requests.value[key] != null }
        val request = SessionPermissions.requests.value.getValue(key)
        assertTrue(SessionPermissions.respond(key, request.id, "n"))
        assertFalse(SessionPermissions.respond(key, request.id, "y"))
        waitUntil { out.readText() == "n" }
        assertEquals("n", out.readText())
        releaseTerminalSession(key)
        assertEquals(null, SessionPermissions.requests.value[key])
    }

    @Test fun manualAnswerMakesAnOldButtonFailLiveScreenValidation() {
        open("printf 'Allow execution? (y/n)'; read answer; printf '\\nFinished\\n'")
        waitUntil { SessionPermissions.requests.value[key] != null }
        val request = SessionPermissions.requests.value.getValue(key)
        assertTrue(typeIntoTerminalSession(key, "n\r"))
        waitUntil { SessionPermissions.requests.value[key] == null }
        assertFalse(SessionPermissions.respond(key, request.id, "y"))
    }

    @Test fun numberedMenuSendsNavigationAndEnterToTheRetainedPty() {
        val out = File.createTempFile("permission-menu", ".txt").apply { deleteOnExit() }
        open("stty -echo -icanon -icrnl; printf 'Allow execution?\\n› 1. Yes, proceed\\n2. No\\n'; dd bs=1 count=4 of='${out.absolutePath}' 2>/dev/null; stty sane")
        waitUntil { SessionPermissions.requests.value[key] != null }
        val request = SessionPermissions.requests.value.getValue(key)
        assertTrue(SessionPermissions.respond(key, request.id, "2"))
        waitUntil { out.length() == 4L }
        assertEquals("\u001b[B\r", out.readText())
    }

    @Test fun partiallyTypedShellCommandsCannotOfferPermissionButtons() {
        open("PS1='$ '")
        assertTrue(typeIntoTerminalSession(key, "echo safe # overwrite? (y/n)"))
        // The existing broad question detector sees the echoed command; the action parser must decline it.
        waitUntil { SessionQuestions.questionFor(key) != null }
        assertEquals(null, SessionPermissions.requests.value[key])
        assertFalse(SessionPermissions.respond(key, "historical-request", "n"))
        assertTrue(typeIntoTerminalSession(key, "\u0003"))
    }

    private fun open(command: String) = acquireDesktopTerminalSession(key, System.getProperty("java.io.tmpdir"),
        "/bin/sh", command, {}, null, 13, 100)

    private fun waitUntil(check: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            if (check()) return
            Thread.sleep(50)
        }
        assertTrue(false, "Timed out waiting for permission PTY")
    }
}
