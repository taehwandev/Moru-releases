package app.thdev.moru.feature.terminal

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeIntoTerminalSessionTest {
    private val key = "type-test-${System.nanoTime()}"

    @AfterTest
    fun release() = releaseTerminalSession(key)

    @Test
    fun textTypedIntoALiveTerminalReachesItsProcess() {
        val out = File.createTempFile("typed", ".txt").apply { deleteOnExit() }
        acquireDesktopTerminalSession(
            sessionKey = key,
            workingDirectory = System.getProperty("java.io.tmpdir"),
            shell = "/bin/sh",
            startupCommand = "read -r line; printf '%s' \"\$line\" > '${out.absolutePath}'",
            onCommandCaptured = {},
            fontFamily = null,
            fontSize = 13,
            scrollbackLines = 100,
        )

        assertTrue(typeIntoTerminalSession(key, "preview problems: boom\n"))

        val deadline = System.nanoTime() + 15_000_000_000L
        while (out.readText().isEmpty() && System.nanoTime() < deadline) Thread.sleep(50)
        assertEquals("preview problems: boom", out.readText())
    }

    @Test
    fun anUnknownTerminalCannotBeTypedInto() {
        assertFalse(typeIntoTerminalSession("no-such-session", "x"))
    }
}
