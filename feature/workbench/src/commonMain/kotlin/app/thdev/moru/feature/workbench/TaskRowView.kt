package app.thdev.moru.feature.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.feature.workbench.sidebar.SidebarSessionSummary
import app.thdev.moru.feature.workbench.sidebar.SidebarSessionSummaryView
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import app.thdev.moru.feature.workbench.sidebar.SidebarStateBadge
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind

/**
 * One Task row with its title and a summary of its actual sessions; the Task itself has no execution state.
 */
@Composable
internal fun TaskRowView(
    task: TaskRow,
    onClick: () -> Unit,
    actions: TaskRowActions = TaskRowActions(),
    accent: MoruIdentityAccent,
    reorderHandle: (@Composable () -> Unit)? = null,
    sessions: List<AgentSessionRow> = emptyList(),
    sessionSummary: SidebarSessionSummary = SidebarSessionSummary.from(sessions),
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val strings = workbenchStrings()
    val description = strings.taskDescription(task.title)
    val rowInteractive = !renaming && !confirmingDelete
    val hasSelectedSession = sessions.any { it.selected }
    val surfaceRole = if (task.selected && !hasSelectedSession) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row
    val finished = sessionSummary.responsesReady > 0
    val awaitingReply = sessionSummary.awaitingReply > 0
    // A question waiting for the user outranks a finished report on the row's edge.
    val edge = when {
        awaitingReply -> MoruTheme.colors.warning
        finished -> MoruTheme.colors.success
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoruTheme.radius.small))
            .ckSurface(surfaceRole)
            .then(if (edge != null) Modifier.drawBehind { drawRect(edge, size = Size(3.dp.toPx(), size.height)) } else Modifier)
            .then(
                if (rowInteractive) {
                    Modifier
                        .clickable(role = Role.Button, onClick = onClick)
                        .semantics(mergeDescendants = true) {
                            selected = task.selected
                            contentDescription = description
                        }
                } else {
                    Modifier.semantics {
                        selected = task.selected
                        contentDescription = description
                    }
                }
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(name = MoruIconName.Tag, tint = accent.color(MoruTheme.colors),
            modifier = Modifier.alignBy { it.measuredHeight / 2 })
        Column(
            modifier = Modifier.weight(1f).alignBy(TaskTitleCenter),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Box(Modifier.layout { measurable, constraints ->
                val title = measurable.measure(constraints)
                layout(title.width, title.height, mapOf(TaskTitleCenter to title.height / 2)) { title.placeRelative(0, 0) }
            }) {
                if (renaming) {
                    InlineNameEditor(
                        initial = task.title,
                        tag = "task-name",
                        onSave = { title -> renaming = false; actions.onRename(task.id, title) },
                        onCancel = { renaming = false },
                    )
                } else {
                    MoruText(
                        text = task.title,
                        style = MoruTheme.typography.body.copy(
                            fontWeight = if (task.selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (task.selected) {
                            MoruTheme.colors.textPrimary
                        } else {
                            MoruTheme.colors.textPrimary.copy(alpha = 0.88f)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Under the title, so a narrow sidebar keeps the whole title and summary width.
            if ((awaitingReply || finished) && !renaming) {
                Row(Modifier.padding(vertical = 1.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (awaitingReply) {
                        SidebarStateBadge(strings.awaitingReply(null), MoruStatusGlyphKind.NeedsYou, MoruTheme.colors.warning,
                            Modifier.testTag("task-awaiting-reply:${task.id}"))
                    }
                    if (finished) {
                        SidebarStateBadge(strings.finished(null), MoruStatusGlyphKind.Done, MoruTheme.colors.success,
                            Modifier.testTag("task-finished:${task.id}"))
                    }
                }
            }
            SidebarSessionSummaryView(sessionSummary, Modifier.testTag("task-session-summary:${task.id}"))
            if (confirmingDelete) {
                TaskDeleteConfirm(
                    onConfirm = { confirmingDelete = false; actions.onDelete(task.id) },
                    onKeep = { confirmingDelete = false },
                )
            }
        }
        if (!renaming) {
            if (reorderHandle != null) {
                Box(Modifier.alignBy { it.measuredHeight / 2 }) { reorderHandle() }
            } else {
                Box(Modifier.alignBy { it.measuredHeight / 2 }) {
                    MoruIconButton(onClick = { menuOpen = true }, description = strings.taskMenu(task.title), size = 20.dp) {
                        MoruIcon(name = MoruIconName.Ellipsis, size = 12.dp)
                    }
                    MoruDropdownMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        items = listOfNotNull(
                            // Only another Task of the selected Task's Project opens beside it; another Project has its own window.
                            actions.onToggleBeside?.takeIf { !task.selected && task.projectId == actions.besideProjectId }?.let { toggle ->
                                MoruMenuItem(if (task.id in actions.besideTaskIds) strings.closeBeside else strings.openBeside) {
                                    toggle(task.id)
                                }
                            },
                            MoruMenuItem(strings.renameTask) { renaming = true },
                            MoruMenuItem(strings.deleteTask) { confirmingDelete = true },
                        ),
                    )
                }
            }
        }
    }
}

private val TaskTitleCenter = HorizontalAlignmentLine(::minOf)
