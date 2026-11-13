package app.thdev.moru.app

import androidx.compose.runtime.Composable
import app.thdev.moru.feature.notifications.NotificationInboxContent
@Composable
internal fun NotificationInboxRoute(
    inbox: NotificationInboxController,
    navigation: WorkbenchNavigation,
    appVersion: String? = null,
    showWorkbench: () -> Unit,
) {
    NotificationInboxContent(
        state = inbox.state.copy(unavailableTaskIds = inbox.state.notifications
            .filterNot { notificationTaskAvailable(it, navigation.sidebar) }.map { it.taskId }.toSet()),
        onOpen = { notification ->
            inbox.markRead(notification.id)
            if (openNotification(notification, navigation)) showWorkbench()
        },
        onDelete = { id -> NotificationPermissionBindings.Current.remove(id); inbox.delete(id) },
        onClear = { NotificationPermissionBindings.Current.clear(); inbox.clear() },
        onMarkAllRead = inbox::markAllRead,
        onRetry = inbox::retry,
        appVersion = appVersion,
        permissionActions = { notification ->
            NotificationPermissionActions(notification, notificationTaskAvailable(notification, navigation.sidebar))
        },
    )
}
