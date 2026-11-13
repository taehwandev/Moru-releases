package app.thdev.moru.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.notifications.formatNotificationCount
/**
 * The notification bell at the right end of the window's title bar row, with the unread count on it. [open] keeps it
 * highlighted while its panel is showing.
 */
@Composable
internal fun TitleBarBell(
    unreadCount: Int,
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoruTheme.colors
    // The name stays "Notifications" so it is always found by it; the unread count is read out as its state.
    val label = appStrings().destinationLabel(MoruDestination.Notifications)
    val unread = if (unreadCount > 0) "${formatNotificationCount(unreadCount)} unread" else ""
    MoruIconButton(
        onClick = onClick,
        description = label,
        modifier = modifier.testTag("notification-inbox-entry").semantics { stateDescription = unread },
        size = 26.dp,
    ) {
        val tint = when {
            unreadCount > 0 -> colors.primary
            open -> colors.textPrimary
            else -> colors.textSecondary
        }
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                drawBell(tint, Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (unreadCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                        .background(colors.primary, RoundedCornerShape(MoruTheme.radius.medium))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MoruText(
                        formatNotificationCount(unreadCount),
                        color = colors.background,
                        style = MoruTheme.typography.micro,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
