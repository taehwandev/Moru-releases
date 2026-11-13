package app.thdev.moru.app

import app.thdev.moru.core.data.InMemoryNotificationRepository
import app.thdev.moru.core.domain.NotificationRepository
import app.thdev.moru.core.model.TaskNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class NotificationInboxControllerTest {
    private val receipt = TaskNotification("n1", "p1", "t1", "Project", "Task", "Task needs you.", 1)

    @Test
    fun recordingReadingAndDeletingOnlyChangeHistory() = runBlocking {
        val repository = InMemoryNotificationRepository()
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val controller = NotificationInboxController(repository, scope)
            controller.record(listOf(receipt))
            assertEquals(listOf(receipt), controller.state.notifications)
            controller.markRead(receipt.id)
            assertTrue(controller.state.notifications.single().read)
            controller.delete(receipt.id)
            assertTrue(controller.state.notifications.isEmpty())
            controller.record(listOf(receipt.copy(id = "n2"), receipt.copy(id = "n3")))
            controller.markAllRead()
            assertTrue(controller.state.notifications.all { it.read })
            controller.clear()
            assertTrue(repository.load().isEmpty())
        } finally { scope.cancel() }
    }

    @Test
    fun aTaskKeepsOneRecordAndReadingItsTaskClearsItFromTheUnreadCount() = runBlocking {
        val repository = InMemoryNotificationRepository()
        var writes = 0
        val counting = object : NotificationRepository by repository {
            override suspend fun markTasksRead(taskIds: Set<String>) { writes++; repository.markTasksRead(taskIds) }
        }
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val controller = NotificationInboxController(counting, scope)
            controller.record(listOf(receipt, receipt.copy(id = "n2", createdAtMillis = 2), receipt.copy(id = "o1", taskId = "t2")))
            assertEquals(listOf("n2", "o1"), controller.state.notifications.map { it.id })
            assertEquals(setOf("t1", "t2"), controller.unreadTaskIds)
            controller.markTasksRead(setOf("t1"))
            assertEquals(setOf("t2"), controller.unreadTaskIds)
            controller.markTasksRead(setOf("t1", "missing"))
            assertEquals(1, writes, "Reading a Task with nothing unread must not write")
        } finally { scope.cancel() }
    }

    @Test
    fun failedReceiptsAndDeletesRemainRetryableWithoutRecursiveRetries() = runBlocking {
        val memory = InMemoryNotificationRepository()
        var fail = true
        val repository = object : NotificationRepository by memory {
            override suspend fun append(notifications: List<TaskNotification>) {
                if (fail) error("Disk unavailable")
                memory.append(notifications)
            }
            override suspend fun delete(id: String) {
                if (fail) error("Disk unavailable")
                memory.delete(id)
            }
        }
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val controller = NotificationInboxController(repository, scope)
            controller.record(listOf(receipt))
            assertNotNull(controller.state.error)
            repeat(3) { controller.retry(); assertNotNull(controller.state.error) }
            fail = false
            controller.retry()
            assertEquals(listOf(receipt), controller.state.notifications)
            assertNull(controller.state.error)
            fail = true
            controller.delete(receipt.id)
            assertEquals(listOf(receipt), controller.state.notifications)
            assertNotNull(controller.state.error)
            assertFalse(controller.state.busy)
            fail = false
            controller.retry()
            assertTrue(controller.state.notifications.isEmpty())
        } finally { scope.cancel() }
    }

    @Test
    fun failedRefreshAfterSuccessfulClearDoesNotClearNewReceiptsOnRetry() = runBlocking {
        val memory = InMemoryNotificationRepository()
        var failLoad = false
        var clears = 0
        val repository = object : NotificationRepository by memory {
            override suspend fun load(): List<TaskNotification> {
                if (failLoad) error("Read unavailable")
                return memory.load()
            }
            override suspend fun clear() { clears++; memory.clear() }
        }
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val controller = NotificationInboxController(repository, scope)
            controller.record(listOf(receipt))
            failLoad = true
            controller.clear()
            assertNotNull(controller.state.error)
            memory.append(listOf(receipt.copy(id = "new")))
            failLoad = false
            controller.retry()
            assertEquals(1, clears)
            assertEquals("new", controller.state.notifications.single().id)
        } finally { scope.cancel() }
    }
}
