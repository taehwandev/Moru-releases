package app.thdev.moru.core.model

/** A local receipt snapshot, never authority for current Task state or actions. */
data class TaskNotification(
    val id: String,
    val projectId: String,
    val taskId: String,
    val projectName: String,
    val taskTitle: String,
    val message: String,
    val createdAtMillis: Long,
    val read: Boolean = false,
)
