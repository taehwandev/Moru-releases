package app.thdev.moru.core.terminal

import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.LimitReached
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopTaskSessionAdmissionTest {
    @Test
    fun simultaneousRequestsCanClaimTheLastSlotOnlyOnce() {
        val manager = DesktopPtyTerminalSessionManager(spawnProcesses = false)
        repeat(TaskTerminalSessionCapacity.Maximum - 1) { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task")) }
        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = (1..2).map {
                executor.submit<Result<TerminalSessionSnapshot>> {
                    ready.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    runCatching { manager.createTaskTerminalSession(TerminalSessionSpec(attachedItemId = "task")) }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            release.countDown()
            val results = requests.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it.isSuccess })
            assertTrue(results.single { it.isFailure }.exceptionOrNull() is LimitReached)
            assertEquals(TaskTerminalSessionCapacity.Maximum, manager.listSessions().size)
            assertEquals(results.single { it.isSuccess }.getOrThrow().id, manager.selectedSessionId())
        } finally {
            release.countDown()
            executor.shutdownNow()
            manager.listSessions().forEach { manager.closeSession(it.id) }
        }
    }
}
