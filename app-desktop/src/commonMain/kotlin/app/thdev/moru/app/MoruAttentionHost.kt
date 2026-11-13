package app.thdev.moru.app

import androidx.compose.runtime.Composable
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.core.system.AttentionNotifier
import app.thdev.moru.core.terminal.TerminalSessionManager

/**
 * The Tasks needing the user, told through notification history and the app icon. Live permission
 * controls remain available independently of receipt read state.
 */
@Composable
internal fun MoruAttentionHost(
    navigation: WorkbenchNavigation,
    sessions: TerminalSessionManager,
    notifier: AttentionNotifier,
    settings: AppSettings,
    onRecord: (List<TaskNotification>) -> Unit,
    viewedTaskId: String?,
    unreadTaskIds: Set<String> = emptySet(),
    onTasksRead: (Set<String>) -> Unit = {},
    showWorkbench: () -> Unit,
) {
    val attention = rememberAttention(navigation, sessions)
    AttentionAlerts(navigation, attention, notifier, notifications = settings.attention.desktopNotifications,
        onRecord = onRecord, viewedTaskId = viewedTaskId, unreadCount = unreadTaskIds.size, onTasksRead = onTasksRead)
}
