package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.ClosedProjectRow
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/**
 * Recently closed projects, folded away until asked for. Pressing one reopens it with the Tasks it had; its cross
 * deletes it from the list after one confirmation. Absent when nothing was closed.
 */
@Composable
internal fun ClosedProjectsSection(
    projects: List<ClosedProjectRow>,
    onReopen: (rootPath: String) -> Unit,
    onForget: (projectId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (projects.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<String?>(null) }
    val strings = workbenchStrings()
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = MoruTheme.spacing.medium, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIcon(name = if (expanded) MoruIconName.ChevronDown else MoruIconName.ChevronRight)
            MoruText(strings.closedProjects(projects.size), style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        }
        if (expanded) {
            projects.forEach { project ->
                val description = strings.reopenProject(project.displayName)
                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = 34.dp, end = MoruTheme.spacing.xSmall),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f).clickable(role = Role.Button) { onReopen(project.rootPath) }
                            .semantics { contentDescription = description }
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MoruText(
                            project.displayName,
                            style = MoruTheme.typography.body,
                            color = MoruTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        MoruIcon(name = MoruIconName.Reopen)
                    }
                    MoruIconButton(
                        onClick = { confirming = project.id },
                        description = strings.forgetClosedProject(project.displayName),
                        size = 28.dp,
                    ) {
                        MoruIcon(name = MoruIconName.Close)
                    }
                }
                if (confirming == project.id) {
                    ClosedProjectForgetConfirm(
                        onConfirm = { confirming = null; onForget(project.id) },
                        onKeep = { confirming = null },
                        modifier = Modifier.padding(start = 34.dp, end = MoruTheme.spacing.medium, bottom = 6.dp),
                    )
                }
            }
        }
    }
}
