package app.thdev.moru.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.moruAmbientBackground

/** The row the window buttons sit in. On macOS it is the title bar; elsewhere a thin strip of its own. */
private val MinTitleRowHeight = 40.dp

/**
 * The persistent app chrome around the selected destination: the rail on the left and, at the right end of the
 * title bar row, a row of icon buttons — Settings and the notification bell. The bell opens
 * [inbox] as a small panel under it, over the current screen; [inbox] gets the action that closes it.
 */
@Composable
internal fun MoruFrame(
    screen: MoruDestination,
    onSelect: (MoruDestination) -> Unit,
    topInset: Dp,
    unreadNotificationCount: Int = 0,
    inbox: @Composable (close: () -> Unit) -> Unit = {},
    /** Floating update feedback anchored below the title row; never resizes the destination. */
    banner: @Composable () -> Unit = {},
    appVersion: String? = null,
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    onCheckForUpdates: (() -> Unit)? = null,
    onRestartForUpdate: () -> Unit = {},
    /** The Project selector and view controls share the native title row. */
    toolbar: @Composable (Modifier) -> Unit = { Spacer(it) },
    content: @Composable () -> Unit,
) {
    var inboxOpen by remember { mutableStateOf(false) }
    val overlay = remember { WindowOverlay() }
    val titleRow = maxOf(topInset, MinTitleRowHeight)
    BoxWithConstraints(Modifier.fillMaxSize().moruAmbientBackground()) {
        Row(Modifier.fillMaxSize()) {
            MoruChrome.PrimaryRail(screen, onSelect = onSelect, topInset = titleRow)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Spacer(Modifier.height(titleRow))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    CompositionLocalProvider(LocalWindowOverlay provides overlay) { content() }
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().height(titleRow)
                .padding(start = if (topInset > 0.dp) 92.dp else 60.dp, end = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            toolbar(Modifier.weight(1f))
            TitleBarSettingsMenu(appVersion, updateStatus, unreadNotificationCount,
                onOpen = { inboxOpen = false },
                onOpenSettings = { inboxOpen = false; onSelect(MoruDestination.Settings) },
                onOpenNotifications = { inboxOpen = true },
                onCheckForUpdates = onCheckForUpdates,
                onRestartForUpdate = onRestartForUpdate)
            TitleBarBell(unreadNotificationCount, open = inboxOpen, onClick = { inboxOpen = !inboxOpen })
        }
        // Below the title row, so the window buttons and toolbar stay reachable.
        WindowReaderOverlay(overlay, titleRow)
        Box(Modifier.align(Alignment.TopEnd).padding(top = titleRow + 8.dp, end = 16.dp)) { banner() }
        if (inboxOpen) {
            val close = { inboxOpen = false }
            NotificationInboxPopover(titleRow, windowWidth = maxWidth, windowHeight = maxHeight, onDismiss = close) { inbox(close) }
        }
    }
}

@Composable
private fun WindowReaderOverlay(overlay: WindowOverlay, titleRow: Dp) {
    val shown = overlay.content ?: return
    val region = overlay.region
    // Do not briefly draw over the sidebar while the workspace's first measurement is pending.
    if (overlay.hasRegion && (region == null || region.width <= 0f || region.height <= 0f)) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fraction = overlay.widthFraction ?: 1f
        val area = with(LocalDensity.current) {
            val left = region?.left?.toDp() ?: 0.dp
            val top = region?.top?.toDp() ?: titleRow
            val width = region?.width?.toDp() ?: maxWidth
            val height = region?.height?.toDp() ?: (maxHeight - titleRow).coerceAtLeast(0.dp)
            Modifier.absoluteOffset(left + width * (1f - fraction), top)
                .requiredSize(width * fraction, height).clipToBounds()
        }
        WindowOverlayLayer(area, shown)
    }
}
