package app.thdev.moru.feature.terminal

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasteIntoTerminalSessionTest {
    private val key = "paste-test-${System.nanoTime()}"

    @AfterTest
    fun release() = releaseTerminalSession(key)

    @Test
    fun aPastedReviewReachesTheProcessLineByLine() {
        val out = File.createTempFile("pasted", ".txt").apply { deleteOnExit() }
        acquireDesktopTerminalSession(
            sessionKey = key,
            workingDirectory = System.getProperty("java.io.tmpdir"),
            shell = "/bin/sh",
            startupCommand = "read -r a; read -r b; printf '%s|%s' \"\$a\" \"\$b\" > '${out.absolutePath}'",
            onCommandCaptured = {},
            fontFamily = null,
            fontSize = 13,
            scrollbackLines = 100,
        )

        assertTrue(pasteIntoTerminalSession(key, "1. docs/PRD.md:3\n   Name the owner\n"))

        val deadline = System.nanoTime() + 15_000_000_000L
        while (out.readText().isEmpty() && System.nanoTime() < deadline) Thread.sleep(50)
        assertEquals("1. docs/PRD.md:3|Name the owner", out.readText())
    }

    @Test
    fun anUnknownTerminalCannotBePastedInto() {
        assertFalse(pasteIntoTerminalSession("no-such-session", "x"))
    }
}
