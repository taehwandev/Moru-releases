package app.thdev.moru.feature.notifications.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.notifications.formatNotificationCount

/** A one-line menu heading like the app's other popovers; global Settings belongs to the persistent app chrome. */
@Composable
internal fun NotificationInboxHeader(
    hasNotifications: Boolean,
    unreadCount: Int,
    idle: Boolean,
    appVersion: String? = null,
    onClear: () -> Unit,
    onMarkAllRead: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruText(
                "Notifications",
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            MoruText(
                "${formatNotificationCount(unreadCount)} unread",
                modifier = Modifier.weight(1f).testTag("notification-unread-count"),
                style = MoruTheme.typography.caption,
                color = if (unreadCount > 0) MoruTheme.colors.warning else MoruTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            appVersion?.takeIf(String::isNotBlank)?.let { version ->
                MoruText("v$version", style = MoruTheme.typography.micro, color = MoruTheme.colors.textSecondary, maxLines = 1)
            }
            MoruIconButton(
                icon = MoruIconName.Check,
                description = "Mark all read",
                onClick = onMarkAllRead,
                modifier = Modifier.testTag("notification-mark-all-read"),
                size = MoruButtonSize.Small,
                enabled = idle && unreadCount > 0,
            )
            MoruIconButton(
                icon = MoruIconName.Trash,
                description = "Clear all",
                onClick = onClear,
                modifier = Modifier.testTag("notification-clear-all"),
                size = MoruButtonSize.Small,
                enabled = idle && hasNotifications,
            )
        }
        MoruDivider(Modifier.padding(horizontal = 2.dp))
    }
}
