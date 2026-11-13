package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.scroll.stableScrollAnchor
import app.thdev.moru.core.model.session.AgentRuntime
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * A collapsed Project's active and just-completed agent sessions, grouped under their Task.
 * Attention gets priority for the bounded four-session preview; admitted rows retain their ownership order.
 */
@Composable
fun ProjectAgentActivity(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    onSelectSession: (AgentSessionRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = sessionsStrings()
    val permissionRequests by SessionPermissions.requests.collectAsState()
    val visibleSessions = rows.filter {
        it.sessionId in permissionRequests ||
            (it.agent is AgentRuntime.Named && (it.activity in InProgressActivities || it.finishedTurn))
    }.sortedBy {
        when {
            it.sessionId in permissionRequests || it.activity == SessionActivity.Asking -> 0
            it.activity == SessionActivity.Starting || it.activity == SessionActivity.Working -> 1
            else -> 2
        }
    }
    if (visibleSessions.isEmpty()) return
    val taskOrder = tasks.keys.withIndex().associate { it.value to it.index }
    val sessionOrder = rows.withIndex().associate { it.value.sessionId to it.index }
    val preview = visibleSessions.take(VisibleLines).sortedWith(
        compareBy<AgentSessionRow> { taskOrder[it.taskKey] ?: Int.MAX_VALUE }
            .thenBy { sessionOrder.getValue(it.sessionId) },
    )
    val guideColor = MoruTheme.colors.border
    Column(
        modifier.fillMaxWidth().padding(top = 4.dp).testTag("project-agent-activity")
            .drawBehind { drawLine(guideColor, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx()) }
            .padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        preview.groupBy { it.taskKey?.takeIf(tasks::containsKey) }.forEach { (taskKey, taskRows) ->
            key(taskKey) {
                val taskTitle = tasks[taskKey]?.taskTitle ?: strings.unattachedGroup
                Column(Modifier.fillMaxWidth().testTag("project-agent-task:${taskKey ?: "unattached"}")) {
                    MoruText(taskTitle, Modifier.stableScrollAnchor("task:$taskKey")
                        .padding(start = 4.dp, bottom = 3.dp),
                        style = MoruTheme.typography.label.copy(fontWeight = FontWeight.Medium),
                        color = MoruTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    taskRows.forEach { row ->
                        key(row.sessionId) {
                            ProjectAgentActivityLine(row, taskTitle) { onSelectSession(row) }
                        }
                    }
                }
            }
        }
        if (visibleSessions.size > VisibleLines) {
            MoruText("+${visibleSessions.size - VisibleLines}", Modifier.padding(start = 6.dp).testTag("project-agent-overflow"),
                style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
        }
    }
}

private val InProgressActivities = setOf(SessionActivity.Starting, SessionActivity.Working, SessionActivity.Asking)

private const val VisibleLines = 4
