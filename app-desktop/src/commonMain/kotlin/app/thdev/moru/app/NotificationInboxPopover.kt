package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruPopover

/**
 * The notification history as a small panel over the current screen, hanging under the title bar bell, instead of
 * a screen of its own. It is sized to the window it opens in ([windowWidth] by [windowHeight]) by
 * [notificationInboxSize]. Clicking outside it or pressing Escape closes it.
 */
@Composable
internal fun BoxScope.NotificationInboxPopover(
    titleRow: Dp,
    windowWidth: Dp,
    windowHeight: Dp,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val (width, height) = notificationInboxSize(titleRow, windowWidth, windowHeight)
    Box(Modifier.align(Alignment.TopEnd).offset(x = -InboxEdgeGap, y = titleRow + 4.dp).size(0.dp)) {
        MoruPopover(expanded = true, onDismiss = onDismiss, scrollable = false) {
            Box(Modifier.width(width).heightIn(max = height).testTag("notification-inbox-popover")) { content() }
        }
    }
}
