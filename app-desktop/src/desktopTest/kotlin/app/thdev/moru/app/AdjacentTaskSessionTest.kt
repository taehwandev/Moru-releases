package app.thdev.moru.app

import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdjacentTaskSessionTest {
    @Test
    fun nextAndPreviousWalkOnlyTheTasksTerminalsAndWrapRound() {
        val sessions = InMemoryTerminalSessionManager(
            listOf(
                TerminalSessionSpec(attachedItemId = "a"),
                TerminalSessionSpec(attachedItemId = "b"),
                TerminalSessionSpec(attachedItemId = "a"),
                TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW, attachedItemId = "a"),
                TerminalSessionSpec(attachedItemId = "a"),
            ),
        )
        val ids = taskTerminalIds(sessions, "a")
        assertEquals(3, ids.size)

        sessions.selectSession(ids[0])
        assertEquals(ids[1], adjacentTaskSession(sessions, "a", 1))
        assertEquals(ids[2], adjacentTaskSession(sessions, "a", -1))
        sessions.selectSession(ids[2])
        assertEquals(ids[0], adjacentTaskSession(sessions, "a", 1))
    }

    @Test
    fun aSelectionInAnotherTaskStartsAtTheEnds() {
        val sessions = InMemoryTerminalSessionManager(
            listOf(TerminalSessionSpec(attachedItemId = "a"), TerminalSessionSpec(attachedItemId = "a"), TerminalSessionSpec(attachedItemId = "b")),
        )
        val ids = taskTerminalIds(sessions, "a")
        assertEquals(ids.first(), adjacentTaskSession(sessions, "a", 1))
        assertEquals(ids.last(), adjacentTaskSession(sessions, "a", -1))
        assertNull(adjacentTaskSession(sessions, "none", 1))
        assertNull(adjacentTaskSession(sessions, null, 1))
    }
}
