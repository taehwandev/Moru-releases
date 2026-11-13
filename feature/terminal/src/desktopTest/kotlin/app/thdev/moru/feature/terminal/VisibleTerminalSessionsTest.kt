package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class VisibleTerminalSessionsTest {
    private fun sessions(count: Int) = (1..count).map { index ->
        TerminalSessionSnapshot(id = "s$index", spec = TerminalSessionSpec(title = "T$index"), status = TerminalSessionStatus.READY)
    }

    private fun ids(list: List<TerminalSessionSnapshot>) = list.map { it.id }

    private fun first(count: Int) = (1..count).map { "s$it" }

    @Test
    fun atMostEightAreDrawnAndTheRestAreLeftToTheirTabs() {
        assertEquals(8, MaxVisibleTerminals)
        assertEquals(first(8), ids(visibleTerminalSessions(sessions(10), selectedSessionId = "s1")))
        assertEquals(listOf("s1", "s2"), ids(visibleTerminalSessions(sessions(2), selectedSessionId = null)))
        assertEquals(emptyList(), ids(visibleTerminalSessions(emptyList(), selectedSessionId = null)))
    }

    @Test
    fun selectingAnotherVisibleTerminalMovesNothing() {
        val all = sessions(10)

        assertEquals(ids(visibleTerminalSessions(all, "s1")), ids(visibleTerminalSessions(all, "s3")))
        assertEquals(ids(visibleTerminalSessions(all, "s1")), ids(visibleTerminalSessions(all, "s8")))
    }

    @Test
    fun aSelectedTerminalOutsideTheFirstEightTakesTheLastPlaceSoItIsAlwaysOnScreen() {
        val all = sessions(10)

        assertEquals(first(7) + "s9", ids(visibleTerminalSessions(all, "s9")))
        assertEquals(first(7) + "s10", ids(visibleTerminalSessions(all, "s10")))
    }

    @Test
    fun anUnknownSelectionChangesNothing() {
        assertEquals(first(8), ids(visibleTerminalSessions(sessions(10), "gone")))
    }
}
