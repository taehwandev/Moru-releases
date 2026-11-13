package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.SidebarView
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** The sidebar's title as a menu, like an IDE's project view picker: it names the view shown and switches to another. */
@Composable
internal fun SidebarViewSwitcher(
    view: SidebarView,
    onViewChange: (SidebarView) -> Unit,
    modifier: Modifier = Modifier,
    reordering: Boolean = false,
    onToggleReorder: (() -> Unit)? = null,
) {
    val strings = workbenchStrings()
    var open by remember { mutableStateOf(false) }
    val label = strings.sidebarViewName(view)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(
                Modifier
                    .selectable(open, role = Role.Button, onClick = { open = true })
                    .semantics { contentDescription = strings.sidebarViewSwitcher(label) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoruText(label, style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                MoruIcon(MoruIconName.ChevronDown, size = 12.dp)
            }
            MoruDropdownMenu(
                expanded = open,
                onDismiss = { open = false },
                items = SidebarView.entries.map { entry ->
                    MoruMenuItem(strings.sidebarViewName(entry), checked = entry == view) { onViewChange(entry) }
                },
            )
        }
        Spacer(Modifier.weight(1f))
        onToggleReorder?.let { toggle ->
            MoruIconButton(
                onClick = toggle,
                description = if (reordering) strings.finishReorderSidebarButton else strings.reorderSidebarButton,
                modifier = Modifier.testTag("sidebar-reorder-toggle"),
                size = 24.dp,
            ) {
                MoruIcon(
                    name = if (reordering) MoruIconName.Check else MoruIconName.ExpandAll,
                    size = 14.dp,
                    tint = if (reordering) MoruTheme.colors.primary else MoruTheme.colors.textSecondary,
                )
            }
        }
    }
}
