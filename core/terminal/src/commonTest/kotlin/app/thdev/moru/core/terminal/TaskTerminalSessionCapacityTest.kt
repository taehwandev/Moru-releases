package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.LimitReached
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.Maximum
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskTerminalSessionCapacityTest {
    @Test
    fun theLastAdmittedSessionFillsTheTaskAndTheNextDoesNotAllocateOrChangeSelection() {
        val manager = InMemoryTerminalSessionManager()
        assertEquals(8, Maximum)
        val admitted = (1..Maximum).map { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-a")) }
        manager.selectSession(admitted.first().id)
        val before = manager.listSessions()
        val revision = manager.revision()
        assertFalse(manager.canAddTaskTerminalSession("task-a"))
        assertFailsWith<LimitReached> {
            manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-a", startupCommand = "must not start"))
        }
        assertEquals(before, manager.listSessions())
        assertEquals(admitted.first().id, manager.selectedSessionId())
        assertEquals(revision, manager.revision())
        val anotherTask = manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-b"))
        assertEquals("term-${Maximum + 1}", anotherTask.id, "Rejection must happen before allocating an id")
    }

    @Test
    fun eachTaskHasItsOwnCapacityAndWebPreviewsDoNotConsumeTerminalSlots() {
        val manager = InMemoryTerminalSessionManager()
        val first = manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-a"))
        manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW, attachedItemId = "task-a"))
        repeat(Maximum) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-b")) }
        repeat(Maximum - 1) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-a")) }
        assertFalse(manager.canAddTaskTerminalSession("task-a"))
        assertFalse(manager.canAddTaskTerminalSession("task-b"))
        val otherTaskIds = manager.listSessions().filter { it.spec.attachedItemId == "task-b" }.map { it.id }
        manager.closeSession(first.id)
        assertTrue(manager.canAddTaskTerminalSession("task-a"))
        manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task-a"))
        assertEquals(otherTaskIds, manager.listSessions().filter { it.spec.attachedItemId == "task-b" }.map { it.id })
    }

    @Test
    fun restorationKeepsEverySessionAndAdmissionResumesOnlyBelowTheMaximum() {
        val manager = InMemoryTerminalSessionManager(List(Maximum + 2) { TerminalSessionSpec(attachedItemId = "legacy-task") })
        val restored = manager.listSessions()
        assertEquals(Maximum + 2, restored.size)
        assertFailsWith<LimitReached> {
            manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "legacy-task"))
        }
        assertEquals(restored, manager.listSessions())
        restored.take(2).forEach { manager.closeSession(it.id) }
        assertFalse(manager.canAddTaskTerminalSession("legacy-task"))
        manager.closeSession(restored[2].id)
        assertTrue(manager.canAddTaskTerminalSession("legacy-task"))
        manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "legacy-task"))
        assertEquals(Maximum, manager.listSessions().size)
        assertEquals(restored.drop(3), manager.listSessions().take(Maximum - 1))
    }
}
