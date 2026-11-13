package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.LimitReached
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.Maximum
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalSessionReassignmentTest {
    @Test
    fun movingChangesOnlyTheOwnerAndPreservesPartialInputAndSelection() {
        val manager = InMemoryTerminalSessionManager()
        val session = manager.createSession(
            TerminalSessionSpec(
                title = "Running terminal",
                shell = "sh",
                workingDirectory = "/workspace/source",
                attachedItemId = "source",
                startupCommand = "start",
                hostKey = "terminal-host-key",
            ),
        )
        manager.writeInput(session.id, "previous command")
        manager.writeRawInput(session.id, "partial ")
        manager.reportOutput(session.id, 1_234)
        manager.reportExit(session.id, 7)
        val other = manager.createSession(TerminalSessionSpec(attachedItemId = "other"))
        val before = manager.listSessions()
        val revision = manager.revision()

        assertTrue(manager.moveSessionToTask(session.id, "target"))

        assertEquals(
            before.map { snapshot ->
                if (snapshot.id == session.id) snapshot.copy(spec = snapshot.spec.copy(attachedItemId = "target"))
                else snapshot
            },
            manager.listSessions(),
        )
        assertEquals(other.id, manager.selectedSessionId())
        assertEquals(revision + 1, manager.revision())

        manager.writeRawInput(session.id, "command\r")
        assertEquals("partial command", manager.listSessions().first().commandHistory.last().command)
    }

    @Test
    fun terminalsAndLabsCanMoveFromAttachedOrUnattachedOwnersWithoutChangingTheirKeys() {
        for (kind in listOf(TerminalSessionKind.TERMINAL, TerminalSessionKind.LAB)) {
            for (owner in listOf("source", null)) {
                val manager = InMemoryTerminalSessionManager()
                val session = manager.createSession(
                    TerminalSessionSpec(kind = kind, attachedItemId = owner, hostKey = "retained-$kind"),
                )
                val revision = manager.revision()

                assertTrue(manager.moveSessionToTask(session.id, "target"))

                assertEquals(session.copy(spec = session.spec.copy(attachedItemId = "target")), manager.listSessions().single())
                assertEquals(session.id, manager.selectedSessionId())
                assertEquals(revision + 1, manager.revision())
            }
        }
    }

    @Test
    fun invalidMovesDoNotChangeSnapshotsRevisionOrSelectionEvenWhenTheOwnerIsFull() {
        val manager = InMemoryTerminalSessionManager()
        val session = manager.createSession(TerminalSessionSpec(attachedItemId = "full"))
        repeat(Maximum - 1) { manager.createSession(TerminalSessionSpec(attachedItemId = "full")) }
        val preview = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW, attachedItemId = "source"))
        val before = manager.listSessions()
        val revision = manager.revision()
        val selected = manager.selectedSessionId()

        assertFalse(manager.moveSessionToTask("missing", "target"))
        assertFalse(manager.moveSessionToTask(session.id, "full"))
        assertFalse(manager.moveSessionToTask(session.id, ""))
        assertFalse(manager.moveSessionToTask(session.id, " \t\n"))
        assertFalse(manager.moveSessionToTask(preview.id, "target"))

        assertEquals(before, manager.listSessions())
        assertEquals(selected, manager.selectedSessionId())
        assertEquals(revision, manager.revision())
    }

    @Test
    fun fullTargetRejectsAttachedTerminalsAndUnattachedLabsBeforeAnyMutation() {
        val manager = InMemoryTerminalSessionManager()
        val terminal = manager.createSession(TerminalSessionSpec(attachedItemId = "source"))
        val lab = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB, hostKey = "conversation"))
        repeat(Maximum) { index ->
            manager.createSession(
                TerminalSessionSpec(
                    kind = if (index % 2 == 0) TerminalSessionKind.TERMINAL else TerminalSessionKind.LAB,
                    attachedItemId = "target",
                ),
            )
        }
        val before = manager.listSessions()
        val revision = manager.revision()
        val selected = manager.selectedSessionId()

        for (session in listOf(terminal, lab)) {
            val error = assertFailsWith<LimitReached> { manager.moveSessionToTask(session.id, "target") }
            assertEquals("target", error.taskKey)
        }

        assertEquals(before, manager.listSessions())
        assertEquals(selected, manager.selectedSessionId())
        assertEquals(revision, manager.revision())
    }

    @Test
    fun aMoveClaimsTheLastSlotIgnoresWebPreviewsAndReleasesItsSourceSlot() {
        val manager = InMemoryTerminalSessionManager()
        val moving = manager.createTaskTerminalSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB, attachedItemId = "source"))
        repeat(Maximum - 1) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "source")) }
        repeat(Maximum - 1) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "target")) }
        manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW, attachedItemId = "target"))
        assertFalse(manager.canAddTaskTerminalSession("source"))
        assertTrue(manager.canAddTaskTerminalSession("target"))

        assertTrue(manager.moveSessionToTask(moving.id, "target"))

        assertTrue(manager.canAddTaskTerminalSession("source"))
        assertFalse(manager.canAddTaskTerminalSession("target"))
        manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "source"))
        assertEquals(Maximum, manager.listSessions().count { it.spec.kind.isTaskSession && it.spec.attachedItemId == "source" })
    }
}
