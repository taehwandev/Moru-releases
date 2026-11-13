package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalWorkspaceCapacityTest {
    @Test
    fun fallbackCreationPreservesSelectionAtCapacityAndWorksAgainAfterClose() {
        val manager = InMemoryTerminalSessionManager()
        val selection = TerminalWorkspaceSelection(manager)
        repeat(TaskTerminalSessionCapacity.Maximum) { assertTrue(selection.create("task-a", "/folder", "/bin/sh", "Terminal ${it + 1}")) }
        val before = manager.listSessions()
        manager.selectSession(before.first().id)
        val selected = selection.selected("task-a", before)
        val reload = selection.reloadVersion
        assertFalse(selection.create("task-a", "/folder", "/bin/sh", "Rejected fifth"))
        assertEquals(before, manager.listSessions())
        assertEquals(selected, selection.selected("task-a", manager.listSessions()))
        assertEquals(selected, manager.selectedSessionId())
        assertEquals(reload, selection.reloadVersion)
        manager.closeSession(before.last().id)
        assertTrue(selection.create("task-a", "/folder", "/bin/sh", "Replacement after close"))
        assertEquals(TaskTerminalSessionCapacity.Maximum, manager.listSessions().size)
        assertTrue(selection.create("task-b", "/other", "/bin/sh", "Independent Task"))
    }
}
