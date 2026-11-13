package app.thdev.moru.feature.notifications.components.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruStatusDot
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.notifications.NotificationInboxRowModel
private val DotSize = 8.dp

/** What a notification row reads: the unread dot, the Task with its Project, the message, and when it arrived. */
@Composable
internal fun NotificationInboxRowContent(row: NotificationInboxRowModel, taskAvailable: Boolean) {
    val notification = row.notification
    val unread = !notification.read
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        if (unread) {
            MoruStatusDot(MoruBadgeType.INFO, Modifier.padding(top = 6.dp).testTag("notification-unread-${notification.id}"), size = DotSize)
        } else {
            Spacer(Modifier.size(DotSize))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoruText(
                    notification.taskTitle,
                    style = MoruTheme.typography.body.copy(fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                MoruText(
                    row.receivedAt,
                    modifier = Modifier.testTag("notification-time-${notification.id}"),
                    color = MoruTheme.colors.textSecondary,
                    style = MoruTheme.typography.caption,
                    maxLines = 1,
                )
            }
            MoruText(
                notification.projectName,
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                color = MoruTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MoruText(
                notification.message,
                modifier = Modifier.testTag("notification-message-${notification.id}"),
                style = MoruTheme.typography.caption,
                color = if (unread) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!taskAvailable) {
                MoruText(
                    "Task unavailable",
                    modifier = Modifier.testTag("notification-unavailable-${notification.id}"),
                    color = MoruTheme.colors.warning,
                    style = MoruTheme.typography.caption,
                )
            }
        }
    }
}
