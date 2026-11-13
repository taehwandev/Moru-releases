package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.ui.identity.projectIdentityAccent
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** Group by stable ownership keys, so identically named Projects and Tasks never blend together. */
@Composable
internal fun SessionBoardCards(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    onSelectSession: (AgentSessionRow, String?) -> Unit,
    onOpenPreview: (String, String) -> Unit,
) {
    val strings = sessionsStrings()
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.groupBy { tasks[it.taskKey]?.projectId }.forEach { (projectId, projectRows) ->
            key(projectId) {
                val projectName = tasks[projectRows.first().taskKey]?.projectName ?: strings.unattachedGroup
                Column(Modifier.fillMaxWidth().testTag("session-board-project:${projectId ?: "unattached"}")) {
                    MoruText(projectName, Modifier.padding(bottom = 2.dp)
                        .testTag("session-board-project-heading:${projectId ?: "unattached"}"),
                        style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = if (projectId == null) MoruTheme.colors.textSecondary
                            else projectIdentityAccent(projectName).color(MoruTheme.colors),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    projectRows.groupBy { it.taskKey?.takeIf(tasks::containsKey) }.forEach { (taskKey, taskRows) ->
                        val task = tasks[taskKey]
                        Column(Modifier.fillMaxWidth().padding(start = 8.dp)
                            .testTag("session-board-task:${taskKey ?: "unattached"}")) {
                            task?.let {
                                MoruText(it.taskTitle, Modifier.padding(top = 4.dp, bottom = 3.dp)
                                    .testTag("session-board-task-heading:$taskKey"),
                                    style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = MoruTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Column(Modifier.fillMaxWidth().padding(start = if (task == null) 0.dp else 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                taskRows.forEach { row ->
                                    key(row.sessionId) {
                                        SessionBoardCard(row, taskKey, onClick = { onSelectSession(row, task?.projectId) },
                                            onOpenPreview = onOpenPreview)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
