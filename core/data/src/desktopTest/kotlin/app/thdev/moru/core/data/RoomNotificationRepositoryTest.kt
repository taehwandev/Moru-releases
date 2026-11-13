package app.thdev.moru.core.data

import app.thdev.moru.core.model.TaskNotification
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomNotificationRepositoryTest {
    private fun record(id: Int) = TaskNotification(
        id = "n$id", projectId = "p$id", taskId = "t$id", projectName = "Project $id",
        taskTitle = "Task $id", message = "Task $id needs you.", createdAtMillis = id.toLong(),
    )

    @Test
    fun receiptSnapshotsReadStateAndDeletionsSurviveReopening() = runBlocking {
        val directory = Files.createTempDirectory("moru-notifications").toFile()
        var repository = defaultDesktopNotificationRepository(directory)
        try {
            repository.append(listOf(record(1), record(2), record(3)))
            repository.markRead("n2")
            repository.delete("n1")
            repository.close()
            repository = defaultDesktopNotificationRepository(directory)
            assertEquals(listOf(record(3), record(2).copy(read = true)), repository.load())
            repository.markAllRead()
            assertTrue(repository.load().all { it.read })
            repository.clear()
            repository.close()
            repository = defaultDesktopNotificationRepository(directory)
            assertTrue(repository.load().isEmpty())
        } finally {
            repository.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun onlyNewestFiveHundredAreRetainedAndDuplicateReceiptsDoNotResetReadState() = runBlocking {
        val directory = Files.createTempDirectory("moru-notification-bounds").toFile()
        val repository = defaultDesktopNotificationRepository(directory)
        try {
            repository.append((1..510).map(::record).reversed())
            assertEquals((510 downTo 11).map(::record), repository.load())
            repository.markRead("n510")
            repository.append(listOf(record(510)))
            assertTrue(repository.load().first().read)
            assertEquals(500, repository.load().size)
            repository.append(listOf(record(511)))
            assertEquals("n511", repository.load().first().id)
            assertFalse(repository.load().any { it.id == "n11" })
        } finally {
            repository.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun aTasksNewRecordReplacesItsEarlierOneAndTasksAreMarkedReadTogether() = runBlocking {
        val directory = Files.createTempDirectory("moru-notification-per-task").toFile()
        val repository = defaultDesktopNotificationRepository(directory)
        try {
            repository.append(listOf(record(1), record(2)))
            repository.markRead("n1")
            val again = record(3).copy(taskId = "t1", message = "Task 1 finished again.")
            repository.append(listOf(again))
            assertEquals(listOf(again, record(2)), repository.load())
            repository.markTasksRead(setOf("t1", "t2"))
            assertTrue(repository.load().all { it.read })
            repository.markTasksRead(emptySet())
        } finally {
            repository.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun invalidBatchDoesNotPartiallyInsertOrDestroyExistingHistory() = runBlocking {
        val directory = Files.createTempDirectory("moru-notification-invalid").toFile()
        val repository = defaultDesktopNotificationRepository(directory)
        try {
            repository.append(listOf(record(1)))
            assertFailsWith<IllegalArgumentException> {
                repository.append(listOf(record(2), record(3).copy(taskId = "")))
            }
            assertEquals(listOf(record(1)), repository.load())
            repository.delete("missing")
            repository.markRead("missing")
            assertEquals(listOf(record(1)), repository.load())
        } finally {
            repository.close()
            directory.deleteRecursively()
        }
    }
}
