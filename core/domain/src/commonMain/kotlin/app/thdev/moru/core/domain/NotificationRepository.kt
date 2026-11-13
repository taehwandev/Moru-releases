package app.thdev.moru.core.domain

import app.thdev.moru.core.model.TaskNotification

/**
 * Local notification history, one record per Task: appending a record replaces that Task's earlier one, and a record
 * whose id is already stored is ignored. Mutations never affect Tasks or OS notifications.
 */
interface NotificationRepository {
    suspend fun load(): List<TaskNotification>
    suspend fun append(notifications: List<TaskNotification>)
    suspend fun markRead(id: String)
    suspend fun markTasksRead(taskIds: Set<String>)
    suspend fun markAllRead()
    suspend fun delete(id: String)
    suspend fun clear()
}
