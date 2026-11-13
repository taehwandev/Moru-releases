package app.thdev.moru.feature.workbench.toolbar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.identity.ProjectIdentityBadge
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState

/** Local menu state only. All Project/session effects belong to the host callbacks. */
@Composable
fun ProjectSwitcher(
    state: ProjectTaskSidebarState,
    onOpenProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onReopenProject: (String) -> Unit,
    sessions: @Composable (dismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var showingSessions by remember { mutableStateOf(false) }
    var anchorBottom by remember { mutableStateOf(0f) }
    val windowSize = LocalWindowInfo.current.containerSize
    val menuHeight = with(LocalDensity.current) {
        (windowSize.height - anchorBottom).coerceAtLeast(0f).toDp()
    }
    val selected = state.projects.firstOrNull { it.id == state.selectedProjectId }
    val label = selected?.displayName ?: "Projects"
    val dismiss = { open = false; showingSessions = false }
    Box(modifier.onGloballyPositioned { anchorBottom = it.boundsInWindow().bottom }) {
        Row(
            Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Control)
                .semantics { contentDescription = "Switch project" }
                .selectable(open, role = Role.Button, onClick = {
                    showingSessions = false
                    open = !open
                }).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected != null) {
                ProjectIdentityBadge(selected.displayName)
            } else {
                MoruIcon(MoruIconName.Folder, size = 14.dp, tint = MoruTheme.colors.textSecondary)
            }
            MoruText(label, modifier = Modifier.weight(1f),
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            MoruIcon(MoruIconName.ChevronDown, size = 12.dp, tint = MoruTheme.colors.textSecondary)
        }
        MoruPopover(open, onDismiss = dismiss, scrollable = false,
            modifier = Modifier.width(360.dp)
                .heightIn(max = (menuHeight - 4.dp).coerceIn(0.dp, 540.dp))) {
            Column(Modifier.fillMaxWidth()) {
                if (showingSessions) {
                    ProjectMenuAction("‹ Projects") { showingSessions = false }
                    sessions(dismiss)
                } else {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        ProjectSwitcherContent(state,
                            onOpenProject = { dismiss(); onOpenProject() },
                            onSelectProject = { dismiss(); onSelectProject(it) },
                            onReopenProject = { dismiss(); onReopenProject(it) },
                            onShowSessions = { showingSessions = true })
                    }
                }
            }
        }
    }
}
