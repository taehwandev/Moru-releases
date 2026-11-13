package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * The sidebar's one control for looking across sessions: a button that opens a popover listing every session,
 * the ones waiting for the user first. It can narrow to the open Project. The tree stays the only permanent view.
 */
@Composable
fun SessionsPopover(
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    selectedProjectId: String?,
    onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    onOpenPreview: (sessionId: String, url: String) -> Unit,
) {
    val strings = sessionsStrings()
    var open by remember { mutableStateOf(false) }
    var thisProjectOnly by remember { mutableStateOf(false) }
    val waiting = rows.count { row -> row.activity.presentation().needsAttention }
    Box {
        Row(
            modifier = Modifier
                .ckSurface(MoruSurfaceRole.Control)
                .selectable(open, role = Role.Button, onClick = { open = true })
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruText(text = strings.title, style = MoruTheme.typography.caption, maxLines = 1)
            if (waiting > 0) {
                MoruText(text = waiting.toString(), style = MoruTheme.typography.caption, color = MoruTheme.colors.warning, maxLines = 1)
            }
        }

        MoruPopover(expanded = open, onDismiss = { open = false }, modifier = Modifier.width(320.dp)) {
            SessionsOverviewContent(rows, tasks, selectedProjectId, thisProjectOnly,
                onScopeChange = { thisProjectOnly = it },
                onSelectSession = { row, projectId -> open = false; onSelectSession(row, projectId) },
                onOpenPreview = { sessionId, url -> open = false; onOpenPreview(sessionId, url) })
        }
    }
}
