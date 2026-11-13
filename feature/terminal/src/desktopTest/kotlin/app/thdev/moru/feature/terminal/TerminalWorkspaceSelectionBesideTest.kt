package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The selected Task's pane and a Task shown beside it, on one session manager: the pane beside remembers its own
 * choice and never moves the app's selected session, so the two panes do not take it from each other.
 */
class TerminalWorkspaceSelectionBesideTest {
    @Test
    fun aPaneBesideNeverMovesTheAppsSelectedSession() {
        val manager = InMemoryTerminalSessionManager()
        val main = TerminalWorkspaceSelection(manager)
        val beside = TerminalWorkspaceSelection(manager, publish = false)
        val a = manager.createSession(TerminalSessionSpec(title = "a", attachedItemId = "task-a", shell = "/bin/sh"))
        val b1 = manager.createSession(TerminalSessionSpec(title = "b1", attachedItemId = "task-b", shell = "/bin/sh"))
        val b2 = manager.createSession(TerminalSessionSpec(title = "b2", attachedItemId = "task-b", shell = "/bin/sh"))
        main.activate("task-a", a.id)
        val revision = manager.revision()

        val besideSessions = listOf(b1, b2).map { created -> manager.listSessions().first { it.id == created.id } }
        beside.activate("task-b", beside.selected("task-b", besideSessions)!!)
        beside.select("task-b", b2.id)

        assertEquals(a.id, manager.selectedSessionId())
        assertEquals(b2.id, beside.selected("task-b", besideSessions))
        assertEquals(revision, manager.revision())
    }

    @Test
    fun aPaneKeepsItsChoiceWhileItsWindowLosesAndRegainsFocus() {
        val manager = InMemoryTerminalSessionManager()
        val pane = TerminalWorkspaceSelection(manager)
        val a1 = manager.createSession(TerminalSessionSpec(title = "a1", attachedItemId = "task-a", shell = "/bin/sh"))
        val a3 = manager.createSession(TerminalSessionSpec(title = "a3", attachedItemId = "task-a", shell = "/bin/sh"))
        val other = manager.createSession(TerminalSessionSpec(title = "p", attachedItemId = "task-p", shell = "/bin/sh"))
        val sessions = listOf(a1, a3).map { created -> manager.listSessions().first { it.id == created.id } }
        pane.select("task-a", a3.id)

        // Another window takes focus and its terminal becomes the app's selected session.
        pane.publish = false
        manager.selectSession(other.id)
        assertEquals(a3.id, pane.selected("task-a", sessions))
        pane.publish = true
        assertEquals(a3.id, pane.selected("task-a", sessions))
    }
}
