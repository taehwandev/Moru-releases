package app.thdev.moru.feature.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruStatusDot
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruPanelCell
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import app.thdev.moru.core.terminal.TerminalSessionSnapshot

/** Stateless tabs preserve selection and layout while the pane's metadata observer sleeps. */
@Composable
internal fun TerminalTabs(
    sessions: List<TerminalSessionSnapshot>,
    selectedSessionId: String?,
    canCreateSplit: Boolean,
    badgeType: (TerminalSessionSnapshot) -> MoruBadgeType,
    onSelectSession: (String) -> Unit,
    onCloseSession: (TerminalSessionSnapshot) -> Unit = {},
    onNewTerminal: () -> Unit,
    /** Other panels among the terminals, such as a device, each with a tab after the terminals'. */
    panels: List<MoruPanelCell> = emptyList(),
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(MoruPaneHeader.height).ckSurface(MoruSurfaceRole.Panel).padding(horizontal = MoruPaneHeader.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sessions.forEach { session ->
                SessionTab(
                    session = session,
                    selected = session.id == selectedSessionId,
                    badgeType = badgeType(session),
                    onClick = { onSelectSession(session.id) },
                    onClose = { onCloseSession(session) },
                )
            }
            panels.forEach { panel -> PanelTab(panel) }
        }
        NewSessionButton(enabled = canCreateSplit, onClick = onNewTerminal)
    }
}

@Composable
private fun SessionTab(
    session: TerminalSessionSnapshot,
    selected: Boolean,
    badgeType: MoruBadgeType,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .ckSurface(if (selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = LocalTerminalSessionLabel.current
        if (label != null) {
            label(session, selected)
        } else {
            MoruStatusDot(type = badgeType, size = 7.dp)
            MoruText(
                text = session.title,
                color = if (selected) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
        MoruIconButton(
            onClick = onClose,
            description = "Close ${session.title}",
            size = 18.dp,
        ) {
            MoruIcon(
                name = MoruIconName.Close,
                size = 10.dp,
                tint = if (selected) MoruTheme.colors.textSecondary else MoruTheme.colors.textSecondary.copy(alpha = 0.7f),
            )
        }
    }
}

/** A panel's tab: its title, and its close button when the panel can be closed from here. */
@Composable
private fun PanelTab(panel: MoruPanelCell) {
    Row(
        modifier = Modifier.ckSurface(MoruSurfaceRole.Row).padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(name = MoruIconName.Smartphone, size = 11.dp, tint = MoruTheme.colors.textSecondary)
        MoruText(
            text = panel.title,
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
        panel.onClose?.let { close ->
            MoruIconButton(onClick = close, description = "Close ${panel.title}", size = 18.dp) {
                MoruIcon(name = MoruIconName.Close, size = 10.dp, tint = MoruTheme.colors.textSecondary.copy(alpha = 0.7f))
            }
        }
    }
}
