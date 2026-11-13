package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.ui.identity.projectIdentityAccent
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** A bounded overview grouped by Project then Task, with attention first within each group. */
@Composable
fun SessionsOverviewContent(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    selectedProjectId: String?,
    thisProjectOnly: Boolean,
    onScopeChange: (Boolean) -> Unit,
    onSelectSession: (AgentSessionRow, String?) -> Unit,
    onOpenPreview: (String, String) -> Unit,
) {
    val strings = sessionsStrings()
    val visible = rows.filter { !thisProjectOnly || (selectedProjectId != null && tasks[it.taskKey]?.projectId == selectedProjectId) }.byUrgency()
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoruText(if (rows.isEmpty()) " " else strings.summary(rows.size, rows.count { it.activity == SessionActivity.Working },
            rows.count { it.activity.presentation().needsAttention }),
            style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SessionScopeOption(strings.scopeAll, selected = !thisProjectOnly) { onScopeChange(false) }
            SessionScopeOption(strings.scopeThisProject, selected = thisProjectOnly) { onScopeChange(true) }
        }
    }
    if (visible.isEmpty()) {
        MoruText(strings.empty, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
    } else {
        Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
            visible.groupBy { tasks[it.taskKey]?.projectId }.forEach { (projectId, projectRows) ->
                val name = tasks[projectRows.first().taskKey]?.projectName ?: strings.unattachedGroup
                MoruText("$name · ${projectRows.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MoruTheme.typography.caption,
                    color = if (projectId == null) MoruTheme.colors.textSecondary
                        else projectIdentityAccent(name).color(MoruTheme.colors))
                projectRows.groupBy { it.taskKey?.takeIf { key -> key in tasks } }.forEach { (taskKey, taskRows) ->
                    SessionTaskGroup(taskKey, tasks[taskKey], taskRows, onSelectSession, onOpenPreview)
                }
            }
        }
    }
}
