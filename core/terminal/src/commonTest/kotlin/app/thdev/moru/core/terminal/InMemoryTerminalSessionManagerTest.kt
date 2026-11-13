package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryTerminalSessionManagerTest {
    @Test
    fun createsAndSelectsMultipleSurfaceKinds() {
        val manager = InMemoryTerminalSessionManager()

        val terminal = manager.createSession(
            TerminalSessionSpec(
                title = "Primary",
                workingDirectory = "/workspace",
            ),
        )
        val web = manager.createSession(
            TerminalSessionSpec(
                title = "Preview",
                kind = TerminalSessionKind.WEB_PREVIEW,
                url = "http://localhost:5173",
            ),
        )

        assertEquals(2, manager.listSessions().size)
        assertEquals(web.id, manager.selectedSessionId())

        manager.selectSession(terminal.id)

        assertEquals(terminal.id, manager.selectedSessionId())
        assertEquals(TerminalSessionKind.WEB_PREVIEW, manager.listSessions().first { it.id == web.id }.spec.kind)
    }

    @Test
    fun closesSelectedSessionAndFallsBackToAnotherSession() {
        val manager = InMemoryTerminalSessionManager(
            initialSessions = listOf(
                TerminalSessionSpec(title = "One"),
                TerminalSessionSpec(title = "Two"),
            ),
        )
        val selected = manager.selectedSessionId()

        assertNotNull(selected)

        manager.closeSession(selected)

        assertEquals(1, manager.listSessions().size)
        assertNotNull(manager.selectedSessionId())

        manager.closeSession(manager.selectedSessionId().orEmpty())

        assertNull(manager.selectedSessionId())
        assertEquals(0, manager.listSessions().size)
    }

    @Test
    fun startupCommandIsRecordedAsInitialInputForHandoffSessions() {
        val manager = InMemoryTerminalSessionManager()

        val session = manager.createSession(
            TerminalSessionSpec(
                title = "Handoff",
                workingDirectory = "/workspace",
                startupCommand = "printf 'handoff ready\\n'",
            ),
        )

        assertTrue(
            session.lines.any { line ->
                line.stream == TerminalOutputStream.INPUT &&
                    line.text == "printf 'handoff ready\\n'"
            },
        )
    }

    @Test
    fun recordsCommandHistoryFromLineAndRawInput() {
        val manager = InMemoryTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "History",
                workingDirectory = "/workspace",
            ),
        )

        manager.writeInput(session.id, "printf 'ok\\n'")
        manager.writeRawInput(session.id, "pwd\rcd ..\r")

        val history = manager.listSessions()
            .single { snapshot -> snapshot.id == session.id }
            .commandHistory

        assertEquals(listOf("printf 'ok\\n'", "pwd", "cd .."), history.map { entry -> entry.command })
        assertEquals(listOf("/workspace", "/workspace", "/workspace"), history.map { entry -> entry.workingDirectory })
    }
}
