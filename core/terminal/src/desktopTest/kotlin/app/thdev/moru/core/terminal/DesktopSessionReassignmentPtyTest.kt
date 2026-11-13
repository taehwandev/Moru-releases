package app.thdev.moru.core.terminal

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopSessionReassignmentPtyTest {
    @Test
    fun movingALivePtyKeepsItsProcessWorkingDirectoryShellStateAndPartialCommand() {
        val directory = Files.createTempDirectory("moru-move-pty").toFile().canonicalFile
        val child = directory.resolve("child").apply { mkdirs() }
        val manager = DesktopPtyTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                shell = "zsh",
                workingDirectory = directory.absolutePath,
                attachedItemId = "source",
            ),
        )
        val beforeCommand =
            "moru_move_value=kept; cd '${child.absolutePath}'; " +
                "printf 'moru-move-before:%s:%s\\n' \"\$\$\" \"\$moru_move_value\""
        val afterCommand =
            "printf 'moru-move-after:%s:%s:%s\\n' \"\$\$\" \"\$moru_move_value\" \"\$PWD\""
        try {
            manager.writeRawInput(session.id, "$beforeCommand\r")
            val beforeOutput = waitForOutput(manager, session.id) { line ->
                Regex("moru-move-before:[0-9]+:kept").matches(line)
            }
            assertNotNull(beforeOutput, "Expected the live shell to report its process id and stored variable")
            val processId = beforeOutput.split(':')[1]
            val before = manager.listSessions().single { it.id == session.id }

            manager.writeRawInput(session.id, afterCommand)
            assertTrue(manager.moveSessionToTask(session.id, "target"))
            val moved = manager.listSessions().single { it.id == session.id }
            assertEquals(session.id, moved.id)
            assertEquals(before.spec.copy(attachedItemId = "target"), moved.spec)
            assertEquals(before.startedAtMillis, moved.startedAtMillis)
            assertEquals(before.commandHistory, moved.commandHistory)
            assertEquals(session.id, manager.selectedSessionId())

            manager.writeRawInput(session.id, "\rexit\r")
            val expected = "moru-move-after:$processId:kept:${child.absolutePath}"
            assertNotNull(
                waitForOutput(manager, session.id) { it == expected },
                "Expected the same PTY process, shell variable, and working directory after moving",
            )
            assertEquals(
                listOf(beforeCommand, afterCommand, "exit"),
                manager.listSessions().single { it.id == session.id }.commandHistory.map { it.command },
                "The partial command must retain its input parser across the move",
            )
        } finally {
            manager.closeSession(session.id)
            directory.deleteRecursively()
        }
    }

    private fun waitForOutput(
        manager: TerminalSessionManager,
        sessionId: String,
        matches: (String) -> Boolean,
    ): String? {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            val line = manager.listSessions().single { it.id == sessionId }.lines
                .firstOrNull { it.stream == TerminalOutputStream.STDOUT && matches(it.text) }
            if (line != null) return line.text
            Thread.sleep(50)
        }
        return null
    }
}
