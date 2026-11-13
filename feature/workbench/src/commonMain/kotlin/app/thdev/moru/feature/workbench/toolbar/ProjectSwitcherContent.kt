package app.thdev.moru.feature.workbench.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDivider
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.path.LocalHomeDirectory
import app.thdev.moru.core.ui.path.homeRelativePath
import app.thdev.moru.feature.workbench.identity.ProjectIdentityBadge
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** Metadata-only lists: showing the menu does not hydrate Projects or start sessions. */
@Composable
internal fun ProjectSwitcherContent(
    state: ProjectTaskSidebarState,
    onOpenProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onReopenProject: (String) -> Unit,
    onShowSessions: () -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    ProjectMenuAction(workbenchStrings().openProjectButton, icon = MoruIconName.FolderOpen, onClick = onOpenProject)
    ProjectMenuAction("Sessions", icon = MoruIconName.Terminal, onClick = onShowSessions)
    if (state.projects.isNotEmpty() || state.closedProjects.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        MoruDivider(Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(2.dp))
    }
    if (state.projects.isNotEmpty()) {
        ProjectMenuHeading("Open projects")
        state.projects.forEach { project ->
            ProjectMenuRow(project.displayName, project.rootPath, project.availabilityLabel,
                selected = project.id == state.selectedProjectId, actionLabel = "Switch to project ${project.displayName}",
                onClick = { onSelectProject(project.id) })
        }
    }
    if (state.closedProjects.isNotEmpty()) {
        ProjectMenuHeading("Recent projects")
        state.closedProjects.forEach { project ->
            ProjectMenuRow(project.displayName, project.rootPath, null,
                selected = false, actionLabel = "Reopen project ${project.displayName}",
                onClick = { onReopenProject(project.rootPath) })
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ProjectMenuHeading(label: String) {
    MoruText(label, color = MoruTheme.colors.textSecondary,
        style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp), maxLines = 1)
}

@Composable
private fun ProjectMenuRow(name: String, path: String?, availability: String?, selected: Boolean, actionLabel: String, onClick: () -> Unit) {
    val colors = MoruTheme.colors
    Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp).fillMaxWidth()
        .background(if (selected) colors.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
            RoundedCornerShape(MoruTheme.radius.small))
        .semantics { contentDescription = actionLabel }
        .selectable(selected, role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProjectIdentityBadge(name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            MoruText(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            (availability ?: path?.let { homeRelativePath(it, LocalHomeDirectory.current) })?.let {
                MoruText(it, style = MoruTheme.typography.caption, color = colors.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (selected) MoruIcon(MoruIconName.Check, size = 14.dp, tint = colors.primary)
    }
}
