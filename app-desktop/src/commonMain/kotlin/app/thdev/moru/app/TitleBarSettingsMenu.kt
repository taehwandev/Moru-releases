package app.thdev.moru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruPopover

/** Anchors app actions to the gear and closes the menu before running an action. */
@Composable
internal fun TitleBarSettingsMenu(
    currentVersion: String?,
    updateStatus: UpdateStatus,
    unreadCount: Int,
    onOpen: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCheckForUpdates: (() -> Unit)?,
    onRestartForUpdate: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    Box {
        TitleBarSettings {
            expanded = !expanded
            if (expanded) onOpen()
        }
        MoruPopover(expanded, onDismiss = { expanded = false },
            modifier = Modifier.width(300.dp).testTag("settings-menu-popover")) {
            SettingsPopoverContent(currentVersion, updateStatus, unreadCount,
                onOpenSettings = { expanded = false; onOpenSettings() },
                onOpenNotifications = { expanded = false; onOpenNotifications() },
                onCheckForUpdates = onCheckForUpdates?.let { check -> { expanded = false; check() } },
                onRestartForUpdate = { expanded = false; onRestartForUpdate() },
                onOpenAbout = { expanded = false; aboutOpen = true })
        }
    }
    if (aboutOpen) AboutMoruDialog(currentVersion, onDismiss = { aboutOpen = false })
}
