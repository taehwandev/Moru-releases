package app.thdev.moru.feature.notifications

import androidx.compose.runtime.Immutable
import app.thdev.moru.core.model.TaskNotification

@Immutable
internal data class NotificationInboxRowModel(
    val notification: TaskNotification,
    val receivedAt: String,
)
