package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** Sessions without a Task remain reachable below the Project groups. */
@Composable
fun UnattachedSessions(
    rows: List<AgentSessionRow>,
    onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit,
    onOpenPreview: (sessionId: String, url: String) -> Unit,
) {
    if (rows.isEmpty()) return
    MoruText(
        text = sessionsStrings().unattachedGroup,
        style = MoruTheme.typography.caption,
        color = MoruTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = MoruTheme.spacing.medium, vertical = MoruTheme.spacing.small),
    )
    rows.forEach { row ->
        SessionRowView(row = row, onClick = { onSelectSession(row, null) }, indent = 12.dp, onOpenPreview = onOpenPreview)
    }
}
