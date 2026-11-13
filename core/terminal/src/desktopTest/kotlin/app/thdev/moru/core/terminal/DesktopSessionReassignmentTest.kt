package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.LimitReached
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.Maximum
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopSessionReassignmentTest {
    @Test
    fun registryMovesPreserveTerminalAndLabStateKeysAndSelection() {
        for (kind in listOf(TerminalSessionKind.TERMINAL, TerminalSessionKind.LAB)) {
            for (owner in listOf("source", null)) {
                val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false, isReaction = { _, _ -> false })
                val session = manager.createSession(
                    TerminalSessionSpec(
                        title = "Retained session",
                        kind = kind,
                        attachedItemId = owner,
                        workingDirectory = "/workspace/source",
                        startupCommand = "original command",
                    ),
                )
                manager.recordCommandHistory(session.id, "previous command")
                manager.reportOutput(session.id, 1_234)
                manager.reportExit(session.id, 7)
                val other = manager.createSession(TerminalSessionSpec(attachedItemId = "other"))
                val before = manager.listSessions()
                val revision = manager.revision()
                assertNotNull(before.first().spec.hostKey)
                assertNotNull(before.first().startedAtMillis)

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
                manager.listSessions().forEach { manager.closeSession(it.id) }
            }
        }
    }

    @Test
    fun invalidAndCapacityRejectedMovesLeaveTheRegistryUntouched() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        val terminal = manager.createSession(TerminalSessionSpec(attachedItemId = "source"))
        val lab = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB))
        repeat(Maximum) { index ->
            manager.createTaskTerminalSession(
                TerminalSessionSpec(
                    kind = if (index % 2 == 0) TerminalSessionKind.TERMINAL else TerminalSessionKind.LAB,
                    attachedItemId = "target",
                ),
            )
        }
        val fullOwnerSession = manager.listSessions().first { it.spec.attachedItemId == "target" }
        val preview = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW))
        val before = manager.listSessions()
        val revision = manager.revision()
        val selected = manager.selectedSessionId()

        assertFalse(manager.moveSessionToTask("missing", "target"))
        assertFalse(manager.moveSessionToTask(fullOwnerSession.id, "target"))
        assertFalse(manager.moveSessionToTask(terminal.id, ""))
        assertFalse(manager.moveSessionToTask(terminal.id, " \t\n"))
        assertFalse(manager.moveSessionToTask(preview.id, "target"))
        for (session in listOf(terminal, lab)) {
            val error = assertFailsWith<LimitReached> { manager.moveSessionToTask(session.id, "target") }
            assertEquals("target", error.taskKey)
        }

        assertEquals(before, manager.listSessions())
        assertEquals(selected, manager.selectedSessionId())
        assertEquals(revision, manager.revision())
        manager.listSessions().forEach { manager.closeSession(it.id) }
    }

    @Test
    fun concurrentMovesCanClaimTheLastTaskSlotOnlyOnce() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        repeat(Maximum - 1) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "target")) }
        val terminal = manager.createSession(TerminalSessionSpec(attachedItemId = "source"))
        val lab = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB))
        val before = manager.listSessions()
        val revision = manager.revision()
        val selected = manager.selectedSessionId()
        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = listOf(terminal, lab).map { session ->
                executor.submit<Result<Boolean>> {
                    ready.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    runCatching { manager.moveSessionToTask(session.id, "target") }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            release.countDown()
            val results = requests.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it.isSuccess })
            assertTrue(results.single { it.isSuccess }.getOrThrow())
            assertTrue(results.single { it.isFailure }.exceptionOrNull() is LimitReached)
            val after = manager.listSessions()
            assertEquals(Maximum, after.count { it.spec.kind.isTaskSession && it.spec.attachedItemId == "target" })
            assertEquals(before.map { it.id }, after.map { it.id })
            val rejected = listOf(terminal, lab)[results.indexOfFirst { it.isFailure }]
            assertEquals(before.single { it.id == rejected.id }, after.single { it.id == rejected.id })
            assertEquals(revision + 1, manager.revision())
            assertEquals(selected, manager.selectedSessionId())
        } finally {
            release.countDown()
            executor.shutdownNow()
            manager.listSessions().forEach { manager.closeSession(it.id) }
        }
    }

    @Test
    fun aMoveAndANewSessionShareTheSameAtomicCapacityAdmission() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        repeat(Maximum - 1) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "target")) }
        val moving = manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.LAB, attachedItemId = "source"))
        manager.createSession(TerminalSessionSpec(kind = TerminalSessionKind.WEB_PREVIEW, attachedItemId = "target"))
        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val move = executor.submit<Result<Boolean>> {
                ready.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                runCatching { manager.moveSessionToTask(moving.id, "target") }
            }
            val create = executor.submit<Result<Boolean>> {
                ready.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                runCatching {
                    manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "target"))
                    true
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            release.countDown()
            val results = listOf(move.get(5, TimeUnit.SECONDS), create.get(5, TimeUnit.SECONDS))
            assertEquals(1, results.count { it.isSuccess })
            assertTrue(results.single { it.isSuccess }.getOrThrow())
            assertTrue(results.single { it.isFailure }.exceptionOrNull() is LimitReached)
            assertEquals(Maximum, manager.listSessions().count { it.spec.kind.isTaskSession && it.spec.attachedItemId == "target" })
        } finally {
            release.countDown()
            executor.shutdownNow()
            manager.listSessions().forEach { manager.closeSession(it.id) }
        }
    }
}
