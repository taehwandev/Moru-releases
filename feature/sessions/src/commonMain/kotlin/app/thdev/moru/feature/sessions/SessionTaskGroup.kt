package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.ui.identity.taskIdentityAccent
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** A Task heading and its sessions; missing Task metadata stays in the unattached section. */
@Composable
internal fun SessionTaskGroup(
    taskKey: String?,
    task: SessionTaskRef?,
    rows: List<AgentSessionRow>,
    onSelectSession: (AgentSessionRow, String?) -> Unit,
    onOpenPreview: (String, String) -> Unit,
) {
    val accent = taskKey?.let(::taskIdentityAccent)
    Column(Modifier.testTag("session-task-group:${taskKey ?: "unattached"}")) {
        if (task != null) {
            MoruText("${task.taskTitle} · ${rows.size}",
                modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = accent?.color(MoruTheme.colors) ?: MoruTheme.colors.textSecondary)
        }
        rows.forEach { row ->
            SessionRowView(row = row, onClick = { onSelectSession(row, task?.projectId) },
                modifier = Modifier.padding(horizontal = MoruTheme.spacing.small, vertical = MoruTheme.spacing.xSmall),
                indent = if (task == null) 12.dp else 24.dp,
                context = task?.let { "${it.projectName} · ${it.taskTitle}" } ?: sessionsStrings().unattachedGroup,
                accent = accent, onOpenPreview = onOpenPreview)
        }
    }
}
