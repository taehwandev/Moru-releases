package app.thdev.moru.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruAlertDialog
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity
import app.thdev.moru.feature.workbench.TaskRow

/** A bounded chooser; its caller rechecks the catalog and session capacity on confirmation. */
@Composable
internal fun SessionTaskMoveDialog(
    sessionId: String,
    title: String,
    workingDirectory: String,
    targets: List<TaskRow>,
    fullTaskIds: Set<String>,
    onDismiss: () -> Unit,
    onMove: (String) -> Result<Unit>,
) {
    var chosen by remember(sessionId) { mutableStateOf<String?>(null) }
    var error by remember(sessionId) { mutableStateOf<String?>(null) }
    val canMove = targets.any { it.id == chosen && it.id !in fullTaskIds }
    MoruAlertDialog(
        onDismiss = onDismiss,
        modifier = Modifier.testTag("session-move-dialog"),
        onKey = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) { onDismiss(); true } else false
        },
    ) {
        MoruText("Move to Task", style = MoruTheme.typography.title)
        MoruText(title, modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        MoruText("The session keeps its conversation and working folder.", modifier = Modifier.padding(top = 8.dp),
            style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        MoruText(workingDirectory, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            style = MoruTheme.typography.micro, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (targets.isEmpty()) {
            MoruText("No other Task is available in this Project. Create a Task first.", style = MoruTheme.typography.caption)
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(targets, key = { it.id }) { task ->
                    val full = task.id in fullTaskIds
                    Row(Modifier.fillMaxWidth()
                        .ckSurface(if (chosen == task.id) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
                        .testTag("session-move-target:${task.id}")
                        .selectable(selected = chosen == task.id, enabled = !full, role = Role.RadioButton) {
                            chosen = task.id; error = null
                        }.padding(8.dp)) {
                        MoruText(task.title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = if (full) MoruTheme.colors.textSecondary else MoruTheme.colors.textPrimary)
                        if (full) MoruText("Full (${TaskTerminalSessionCapacity.Maximum})", style = MoruTheme.typography.micro)
                    }
                }
            }
        }
        error?.let { MoruText(it, modifier = Modifier.padding(top = 8.dp), style = MoruTheme.typography.caption) }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
            MoruButton("Cancel", onDismiss, type = MoruButtonType.SECONDARY)
            MoruButton("Move", enabled = canMove, onClick = {
                chosen?.let { target -> onMove(target).onFailure { error = it.message ?: "Could not move this session." } }
            })
        }
    }
}
