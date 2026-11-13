package app.thdev.moru.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.navigation.MoruSegmentedControl
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState
import app.thdev.moru.feature.workbench.toolbar.ProjectSwitcher

/** One line in the window title row. Project metadata belongs to the status bar. */
@Composable
internal fun ProjectHeader(
    state: ProjectTaskSidebarState,
    taskTitle: String?,
    selected: ProjectTab,
    onSelect: (ProjectTab) -> Unit,
    onOpenProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onReopenProject: (String) -> Unit,
    sessions: @Composable (dismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = appStrings()
    BoxWithConstraints(modifier) {
        val toolbarWidth = maxWidth
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProjectSwitcher(state, onOpenProject, onSelectProject, onReopenProject, sessions,
                Modifier.width((toolbarWidth - 220.dp).coerceIn(72.dp, 220.dp)))
            if (toolbarWidth >= 560.dp && taskTitle != null) {
                MoruText(taskTitle, modifier = Modifier.weight(1f), style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Spacer(Modifier.weight(1f))
            if (!state.isEmpty) {
                MoruSegmentedControl(options = ProjectTab.entries, selected = selected, onSelect = onSelect,
                    label = { strings.tabLabel(it) })
            }
        }
    }
}
