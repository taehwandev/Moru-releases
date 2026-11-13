package app.thdev.moru.core.data

import app.thdev.moru.core.domain.NotificationRepository
import app.thdev.moru.core.model.TaskNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Isolated history for previews/tests; production uses the Room repository. */
class InMemoryNotificationRepository : NotificationRepository {
    private val records = MutableStateFlow<List<TaskNotification>>(emptyList())
    private val mutex = Mutex()

    override suspend fun load() = mutex.withLock { records.value }

    override suspend fun append(notifications: List<TaskNotification>) = mutex.withLock {
        var next = records.value
        notifications.forEach { notification ->
            if (next.none { it.id == notification.id }) next = next.filterNot { it.taskId == notification.taskId } + notification
        }
        records.value = next.sortedByDescending { it.createdAtMillis }.take(500)
    }

    override suspend fun markRead(id: String) = mutex.withLock {
        records.value = records.value.map { if (it.id == id) it.copy(read = true) else it }
    }

    override suspend fun markTasksRead(taskIds: Set<String>) = mutex.withLock {
        records.value = records.value.map { if (it.taskId in taskIds) it.copy(read = true) else it }
    }

    override suspend fun markAllRead() = mutex.withLock {
        records.value = records.value.map { it.copy(read = true) }
    }

    override suspend fun delete(id: String) = mutex.withLock {
        records.value = records.value.filterNot { it.id == id }
    }

    override suspend fun clear() = mutex.withLock { records.value = emptyList() }
}
