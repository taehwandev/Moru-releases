package app.thdev.moru.feature.notifications

import androidx.compose.runtime.Immutable
import app.thdev.moru.core.model.TaskNotification

/** A caller-owned snapshot; inbox actions never change Task or approval state. */
@Immutable
data class NotificationInboxUiState(
    val loading: Boolean = true,
    val notifications: List<TaskNotification> = emptyList(),
    val unavailableTaskIds: Set<String> = emptySet(),
    val error: String? = null,
    val busy: Boolean = false,
)
