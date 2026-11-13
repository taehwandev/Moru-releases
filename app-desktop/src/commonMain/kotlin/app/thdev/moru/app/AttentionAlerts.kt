package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalWindowInfo
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.core.system.AttentionNotifier
import app.thdev.moru.core.terminal.SESSION_WORKING_WINDOW_MILLIS
import kotlinx.coroutines.delay

/**
 * Keeps the notification history in step with the Tasks that need the user. Each new attention episode is recorded,
 * already read when the user is looking at that Task in a focused window, and only an unread one reaches the system
 * notifications when [notifications] is on. A Task the user looks at, or that stops needing them, has its record marked
 * read, so [unreadCount], which the app icon shows, is the same number the inbox shows. Draws nothing.
 */
@Composable
internal fun AttentionAlerts(
    navigation: WorkbenchNavigation,
    attention: AttentionState,
    notifier: AttentionNotifier,
    notifications: Boolean,
    onRecord: (List<TaskNotification>) -> Unit,
    viewedTaskId: String?,
    unreadCount: Int = 0,
    onTasksRead: (Set<String>) -> Unit = {},
) {
    val strings = appStrings()
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val policy = remember { AttentionPolicy() }
    val record = rememberUpdatedState(onRecord)
    val read = rememberUpdatedState(onTasksRead)
    val systemAlerts = rememberUpdatedState(notifications)
    val latestCandidates = rememberUpdatedState(attention.candidates)
    val looking = rememberUpdatedState(viewedTaskId?.takeIf { windowFocused })
    val candidates = attention.candidates

    fun step() {
        val fresh = policy.update(latestCandidates.value, viewedTaskId = null, windowFocused = false)
        val viewed = looking.value
        if (fresh.isNotEmpty()) {
            record.value(fresh.mapNotNull { candidate ->
                attentionNotification(candidate, navigation.sidebar, strings.attentionMessage(candidate),
                    NotificationReceipt.id(), NotificationReceipt.now())?.copy(read = candidate.taskId == viewed)
            })
            if (systemAlerts.value) fresh.filterNot { it.taskId == viewed }.forEach { candidate ->
                notifier.show(strings.notificationTitle, strings.attentionMessage(candidate))
            }
        }
        if (policy.ended.isNotEmpty()) read.value(policy.ended)
    }

    LaunchedEffect(unreadCount) { notifier.badge(unreadCount) }
    LaunchedEffect(candidates, windowFocused) { step() }
    // An episode ends only after attention has stayed away for the working window, which no change of candidates marks.
    LaunchedEffect(Unit) {
        while (true) {
            delay(SESSION_WORKING_WINDOW_MILLIS)
            step()
        }
    }
    LaunchedEffect(viewedTaskId, windowFocused, unreadCount) {
        looking.value?.let { taskId -> read.value(setOf(taskId)) }
    }
}
