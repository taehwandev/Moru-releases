package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Compact app actions; opening this content only reads the existing update and notification state. */
@Composable
internal fun SettingsPopoverContent(
    currentVersion: String?,
    updateStatus: UpdateStatus,
    unreadCount: Int,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCheckForUpdates: (() -> Unit)?,
    onRestartForUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAbout: () -> Unit = {},
) {
    val strings = appStrings()
    val version = currentVersion?.takeIf(String::isNotBlank)
    val busy = updateStatus is UpdateStatus.Checking || updateStatus is UpdateStatus.Downloading || updateStatus is UpdateStatus.Installing
    Column(modifier.fillMaxWidth().testTag("moru-settings-menu").padding(6.dp)) {
        if (updateStatus is UpdateStatus.Ready) {
            SettingsMenuAction("Update to ${updateStatus.version} and restart", MoruIconName.Reopen,
                onRestartForUpdate, prominent = true, modifier = Modifier.testTag("app-menu-restart"))
        } else {
            SettingsMenuAction(strings.menuCheckForUpdates, MoruIconName.Download, { onCheckForUpdates?.invoke() },
                enabled = version != null && onCheckForUpdates != null && !busy,
                modifier = Modifier.testTag("app-menu-check-updates"))
            val message = if (version == null || onCheckForUpdates == null) "Updates require an installed Moru app."
                else updateMessage(strings, updateStatus)
            message?.let {
                MoruText(it, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MoruTheme.typography.caption,
                    color = if (updateStatus is UpdateStatus.Failed) MoruTheme.colors.warning else MoruTheme.colors.textSecondary)
            }
        }
        val unread = unreadCount.coerceAtLeast(0)
        SettingsMenuAction("Notifications", MoruIconName.Bell, onOpenNotifications,
            trailing = unread.takeIf { it > 0 }?.toString(), attention = unread > 0,
            modifier = Modifier.testTag("app-menu-notifications"))
        SettingsMenuAction("${strings.menuSettings}…", MoruIconName.Settings, onOpenSettings,
            trailing = "⌘,", modifier = Modifier.testTag("app-menu-settings"))
        SettingsMenuAction("About Moru", MoruIconName.About, onOpenAbout,
            modifier = Modifier.testTag("app-menu-about"))
        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(1.dp).background(MoruTheme.colors.border))
        MoruText(appVersionLabel(version), modifier = Modifier.testTag("app-menu-version").padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
            style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
    }
}

@Composable
private fun SettingsMenuAction(
    label: String,
    icon: MoruIconName,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = false,
    attention: Boolean = false,
    trailing: String? = null,
) {
    val colors = MoruTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint = when {
        !enabled -> colors.textSecondary
        attention -> colors.warning
        prominent -> colors.primary
        else -> colors.textPrimary
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = 34.dp)
            .background(if (prominent) colors.primary.copy(alpha = if (colors.isLight) .12f else .22f)
                else if (hovered && enabled) colors.primary.copy(alpha = .10f) else Color.Transparent, RoundedCornerShape(MoruTheme.radius.medium))
            .hoverable(interaction)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(icon, size = 16.dp, tint = if (prominent || attention) tint else colors.textSecondary)
        MoruText(label, modifier = Modifier.weight(1f), color = tint,
            style = MoruTheme.typography.body.copy(fontWeight = if (prominent) FontWeight.SemiBold else FontWeight.Normal))
        trailing?.let {
            MoruText(it, color = if (attention) colors.warning else colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontWeight = if (attention) FontWeight.SemiBold else FontWeight.Normal))
        }
    }
}
