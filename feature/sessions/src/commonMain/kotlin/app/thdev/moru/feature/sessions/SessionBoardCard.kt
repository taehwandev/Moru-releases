package app.thdev.moru.feature.sessions

import androidx.compose.runtime.Composable
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.ui.identity.taskIdentityAccent

/** Ownership lives in the board headings, leaving each session's work an uninterrupted line. */
@Composable
internal fun SessionBoardCard(
    row: AgentSessionRow,
    taskKey: String?,
    onClick: () -> Unit,
    onOpenPreview: (String, String) -> Unit,
) {
    SessionRowView(row, onClick, accent = taskKey?.let(::taskIdentityAccent), onOpenPreview = onOpenPreview)
}
