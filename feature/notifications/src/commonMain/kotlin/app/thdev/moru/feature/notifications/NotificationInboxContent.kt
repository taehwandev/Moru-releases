package app.thdev.moru.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.feature.notifications.components.feedback.NotificationInboxEmpty
import app.thdev.moru.feature.notifications.components.feedback.NotificationInboxError
import app.thdev.moru.feature.notifications.components.feedback.NotificationInboxProgress
import app.thdev.moru.feature.notifications.components.lists.NotificationInboxRow
import app.thdev.moru.feature.notifications.components.navigation.NotificationInboxHeader

/** Renders durable history at its content height, scrolling within the caller's available space. */
@Composable
fun NotificationInboxContent(
    state: NotificationInboxUiState,
    onOpen: (TaskNotification) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    onMarkAllRead: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    appVersion: String? = null,
    permissionActions: @Composable (TaskNotification) -> Unit = {},
) {
    val language = LocalUiLanguage.current
    val rows = remember(state.notifications, language) {
        state.notifications.map { notification ->
            NotificationInboxRowModel(notification, notificationTimestampLabel(notification.createdAtMillis, language))
        }
    }
    val idle = !state.loading && !state.busy
    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("notification-inbox"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
    ) {
        item(key = "header") {
            NotificationInboxHeader(
                hasNotifications = rows.isNotEmpty(),
                unreadCount = rows.count { !it.notification.read },
                idle = idle,
                appVersion = appVersion,
                onClear = onClear,
                onMarkAllRead = onMarkAllRead,
            )
        }
        state.error?.let { error ->
            item(key = "error") {
                NotificationInboxError(error, retryEnabled = idle, onRetry = onRetry)
            }
        }
        if (state.loading || state.busy) {
            item(key = "progress") { NotificationInboxProgress(loading = state.loading) }
        }
        if (rows.isEmpty() && !state.loading && state.error == null) {
            item(key = "empty") { NotificationInboxEmpty() }
        }
        items(rows, key = { "notification:${it.notification.id}" }) { row ->
            NotificationInboxRow(
                row = row,
                taskAvailable = row.notification.taskId !in state.unavailableTaskIds,
                idle = idle,
                onOpen = { onOpen(row.notification) },
                onDelete = { onDelete(row.notification.id) },
                permissionActions = { permissionActions(row.notification) },
            )
        }
    }
}
