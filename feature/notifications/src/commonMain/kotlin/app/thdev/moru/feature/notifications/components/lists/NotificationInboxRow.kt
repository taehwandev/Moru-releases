package app.thdev.moru.feature.notifications.components.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.notifications.NotificationInboxRowModel
import androidx.compose.ui.unit.dp

/**
 * One Task's latest notification as a flat menu row that tints on hover, like the app's other popover lists. The row opens the Task; the cross deletes the record. A Task that no
 * longer exists cannot be opened.
 */
@Composable
internal fun NotificationInboxRow(
    row: NotificationInboxRowModel,
    taskAvailable: Boolean,
    idle: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    permissionActions: @Composable () -> Unit = {},
) {
    val notification = row.notification
    val colors = MoruTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint = when {
        hovered && idle && taskAvailable -> colors.primary.copy(alpha = .10f)
        !notification.read -> colors.primary.copy(alpha = if (colors.isLight) .05f else .08f)
        else -> Color.Transparent
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(tint, RoundedCornerShape(MoruTheme.radius.small))
            .hoverable(interaction)
            .testTag("notification-row-${notification.id}"),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // The open target lies over the content so the texts keep their own semantics.
            Box(Modifier.weight(1f)) {
                NotificationInboxRowContent(row, taskAvailable)
                Box(
                    Modifier.matchParentSize()
                        .clickable(enabled = idle && taskAvailable, role = Role.Button, interactionSource = interaction, indication = null, onClick = onOpen)
                        .semantics { contentDescription = "Open task: ${notification.taskTitle} in ${notification.projectName}" }
                        .testTag("notification-open-${notification.id}"),
                )
            }
            MoruIconButton(
                icon = MoruIconName.Close,
                description = "Delete notification for ${notification.taskTitle} in ${notification.projectName}",
                onClick = onDelete,
                enabled = idle,
                size = MoruButtonSize.Small,
                modifier = Modifier.padding(end = 4.dp).testTag("notification-delete-${notification.id}"),
            )
        }
        // Choice buttons must stay outside the Task-open overlay and delete target.
        if (idle && taskAvailable) permissionActions()
    }
}
