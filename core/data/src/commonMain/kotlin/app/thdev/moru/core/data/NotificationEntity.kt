package app.thdev.moru.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.thdev.moru.core.model.TaskNotification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val taskId: String,
    val projectName: String,
    val taskTitle: String,
    val message: String,
    val createdAtMillis: Long,
    val read: Boolean,
) {
    fun toModel() = TaskNotification(id, projectId, taskId, projectName, taskTitle, message, createdAtMillis, read)

    companion object {
        fun from(model: TaskNotification) = NotificationEntity(
            model.id, model.projectId, model.taskId, model.projectName, model.taskTitle,
            model.message, model.createdAtMillis, model.read,
        )
    }
}
