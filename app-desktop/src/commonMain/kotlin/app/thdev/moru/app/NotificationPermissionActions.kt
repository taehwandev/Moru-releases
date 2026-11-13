package app.thdev.moru.app

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.sessions.SessionPermissionActions

/** Only the request captured when this receipt was generated can authorize a response. */
@Composable
internal fun NotificationPermissionActions(notification: TaskNotification, taskAvailable: Boolean) {
    val requests by SessionPermissions.requests.collectAsState()
    val binding = NotificationPermissionBindings.Current.resolve(notification.id, taskAvailable) { requests[it]?.id }
        ?: return
    SessionPermissionActions(
        sessionId = binding.sessionId,
        expectedRequestId = binding.requestId,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        onResponded = { NotificationPermissionBindings.Current.remove(notification.id) },
    )
}
