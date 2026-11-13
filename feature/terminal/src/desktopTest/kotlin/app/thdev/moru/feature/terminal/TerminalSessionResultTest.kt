package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.SessionResults
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalSessionResultTest {
    private val key = "result-test-${System.nanoTime()}"

    @AfterTest
    fun release() = releaseTerminalSession(key)

    @Test
    fun aFinishedTurnOnScreenBecomesTheResultAndAReplyFromTheCardReachesTheAgent() {
        val answers = File.createTempFile("answers", ".txt").apply { deleteOnExit() }
        val agent = File.createTempFile("agent", ".sh").apply {
            deleteOnExit()
            writeText(
                """
                printf '⏺ Fixed the preview links.\n\n  Should I also update the rail?\n\n✻ Churned for 2s\n─────────\n❯ '
                IFS= read -r line; printf '%s' "${'$'}line" > '${answers.absolutePath}'
                printf '\n⏺ Updated the rail too.\n\n✻ Worked for 1s\n─────────\n❯ '
                sleep 30
                """.trimIndent(),
            )
        }
        acquireDesktopTerminalSession(
            sessionKey = key,
            workingDirectory = System.getProperty("java.io.tmpdir"),
            shell = "/bin/sh",
            startupCommand = "sh '${agent.absolutePath}'",
            onCommandCaptured = {},
            fontFamily = null,
            fontSize = 13,
            scrollbackLines = 100,
        )

        waitFor { SessionResults.resultFor(key) != null }
        assertEquals("Fixed the preview links.\n\nShould I also update the rail?", SessionResults.resultFor(key))

        assertTrue(SessionResults.send(key, "Yes"))
        waitFor { answers.readText().isNotEmpty() }
        assertEquals("Yes", answers.readText())
        waitFor { SessionResults.resultFor(key) == "Updated the rail too." }
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(50)
        assertTrue(condition(), "timed out")
    }
}
