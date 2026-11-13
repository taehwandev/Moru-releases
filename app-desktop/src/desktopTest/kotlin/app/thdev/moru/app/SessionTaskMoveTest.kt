package app.thdev.moru.app

import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import app.thdev.moru.feature.workbench.restore.restoreTerminals
import app.thdev.moru.feature.workbench.restore.savedTerminalsOf
import kotlin.test.*

class SessionTaskMoveTest {
    @Test
    fun moveSelectsDestinationPreservesRequestAndRestoresNewOwnershipForTerminalAndLab() {
        ProjectToolbarFixture().use { f ->
            val destination = f.catalog.createTask(f.projectA.id, "Destination", "Destination request")
            val navigation = WorkbenchNavigation(f.catalog)
            TerminalSessionKind.entries.filter { it.isTaskSession }.forEach { kind ->
                val session = f.sessions.createSession(TerminalSessionSpec(title = "Working", kind = kind,
                    workingDirectory = f.alpha.path, attachedItemId = f.taskA.id, hostKey = "key-$kind"))
                assertTrue(SessionTaskMove.move(navigation, f.sessions, session.id, f.taskA.id, destination.id).isSuccess)
                assertEquals(destination.id, navigation.sidebar.selectedTaskId)
                assertEquals(session.id, f.sessions.selectedSessionId())
                assertEquals("Read Alpha", f.catalog.loadTask(f.taskA.id)?.desiredOutcome)
                assertEquals("Destination request", f.catalog.loadTask(destination.id)?.desiredOutcome)
                val moved = f.sessions.listSessions().first { it.id == session.id }
                assertEquals(session.copy(spec = session.spec.copy(attachedItemId = destination.id)), moved)
            }
            val restored = InMemoryTerminalSessionManager()
            restoreTerminals(restored, WorkbenchLayout(terminals = savedTerminalsOf(f.sessions.listSessions()), selectedTerminal = 1))
            assertEquals(listOf(destination.id, destination.id), restored.listSessions().map { it.spec.attachedItemId })
            assertEquals(listOf("key-TERMINAL", "key-LAB"), restored.listSessions().map { it.spec.hostKey })
            navigation.deleteTaskWithSessions(f.taskA.id, f.sessions)
            assertEquals(2, f.sessions.listSessions().size, "Deleting the source Task leaves moved sessions alive")
        }
    }

    @Test
    fun rejectsOtherProjectUnavailableProjectAndDeletedDestinationWithoutChangingSession() {
        ProjectToolbarFixture().use { f ->
            val destination = f.catalog.createTask(f.projectA.id, "Destination", "")
            val session = f.sessions.createSession(TerminalSessionSpec(attachedItemId = f.taskA.id))
            val navigation = WorkbenchNavigation(f.catalog)
            assertTrue(SessionTaskMove.move(navigation, f.sessions, session.id, f.taskA.id, f.taskB.id).isFailure)
            f.catalog.deleteTask(destination.id)
            assertTrue(SessionTaskMove.move(navigation, f.sessions, session.id, f.taskA.id, destination.id).isFailure)
            val unavailable = f.catalog.createTask(f.projectA.id, "Unavailable destination", "")
            assertTrue(f.alpha.delete())
            assertTrue(SessionTaskMove.move(navigation, f.sessions, session.id, f.taskA.id, unavailable.id).isFailure)
            assertEquals(session, f.sessions.listSessions().single())
            assertEquals(f.taskA.id, navigation.sidebar.selectedTaskId)
        }
    }

    @Test
    fun rejectsClosedSessionChangedOwnerAndFullDestination() {
        ProjectToolbarFixture().use { f ->
            val target = f.catalog.createTask(f.projectA.id, "Target", "")
            val elsewhere = f.catalog.createTask(f.projectA.id, "Elsewhere", "")
            val navigation = WorkbenchNavigation(f.catalog)
            val session = f.sessions.createSession(TerminalSessionSpec(attachedItemId = f.taskA.id))
            f.sessions.moveSessionToTask(session.id, elsewhere.id)
            assertTrue(SessionTaskMove.move(navigation, f.sessions, session.id, f.taskA.id, target.id).isFailure)
            repeat(TaskTerminalSessionCapacity.Maximum) { f.sessions.createSession(TerminalSessionSpec(attachedItemId = target.id)) }
            assertIs<TaskTerminalSessionCapacity.LimitReached>(SessionTaskMove.move(navigation, f.sessions, session.id, elsewhere.id, target.id).exceptionOrNull())
            assertEquals(elsewhere.id, f.sessions.listSessions().first().spec.attachedItemId)
            f.sessions.closeSession(session.id)
            assertTrue(SessionTaskMove.move(navigation, f.sessions, session.id, elsewhere.id, target.id).isFailure)
        }
    }

    @Test
    fun destinationCompletionIsNotChangedByMovingASession() {
        ProjectToolbarFixture().use { f ->
            val target = f.catalog.createTask(f.projectA.id, "Finished", "")
            f.catalog.updateTaskStatus(target.id, TaskStatus.Done)
            val session = f.sessions.createSession(TerminalSessionSpec(attachedItemId = f.taskA.id))
            assertTrue(SessionTaskMove.move(WorkbenchNavigation(f.catalog), f.sessions, session.id, f.taskA.id, target.id).isSuccess)
            assertEquals(TaskStatus.Done, f.catalog.loadTask(target.id)?.status)
        }
    }
}
