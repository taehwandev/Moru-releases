package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** The sidebar's title with the things it owns: the list of every session, reordering mode, and opening a project. */
@Composable
internal fun SidebarHeader(
    onOpenProject: () -> Unit,
    sessions: @Composable () -> Unit = {},
    reordering: Boolean = false,
    onToggleReorder: (() -> Unit)? = null,
) {
    val strings = workbenchStrings()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(
            strings.workspaceTitle,
            style = MoruTheme.typography.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        sessions()
        onToggleReorder?.let { toggle ->
            MoruIconButton(
                onClick = toggle,
                description = if (reordering) strings.finishReorderSidebarButton else strings.reorderSidebarButton,
                modifier = Modifier.testTag("sidebar-reorder-toggle"),
                size = 26.dp,
            ) {
                MoruIcon(
                    name = if (reordering) MoruIconName.Check else MoruIconName.ExpandAll,
                    size = 15.dp,
                    tint = if (reordering) MoruTheme.colors.primary else MoruTheme.colors.textSecondary,
                )
            }
        }
        MoruIconButton(onClick = onOpenProject, description = strings.openProjectButton, size = 26.dp) {
            MoruIcon(name = MoruIconName.FolderPlus, size = 15.dp, tint = MoruTheme.colors.textPrimary)
        }
    }
}

