package app.thdev.moru.core.data

import app.thdev.moru.core.domain.NotificationRepository
import app.thdev.moru.core.model.TaskNotification

class RoomNotificationRepository(private val database: MoruNotificationDatabase) : NotificationRepository {
    private val dao = database.notificationDao()

    override suspend fun load() = dao.load().map(NotificationEntity::toModel)

    override suspend fun append(notifications: List<TaskNotification>) {
        notifications.forEach {
            require(it.id.isNotBlank() && it.projectId.isNotBlank() && it.taskId.isNotBlank()) {
                "Notification identifiers must be present."
            }
        }
        if (notifications.isNotEmpty()) dao.appendBounded(notifications.map(NotificationEntity::from))
    }

    override suspend fun markRead(id: String) = dao.markRead(id)
    override suspend fun markTasksRead(taskIds: Set<String>) {
        if (taskIds.isNotEmpty()) dao.markTasksRead(taskIds.toList())
    }
    override suspend fun markAllRead() = dao.markAllRead()
    override suspend fun delete(id: String) = dao.delete(id)
    override suspend fun clear() = dao.clear()

    fun close() = database.close()
}
