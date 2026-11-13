package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.sessions.SessionTabLabel
import app.thdev.moru.feature.sessions.rememberSessionLabelRows
import app.thdev.moru.feature.terminal.rememberTerminalRevision

/**
 * The label a terminal tab and pane header draw for each session, from the same rows as the sidebar. The lambda
 * stays the same while the rows change, so a clock tick redraws only the labels and not the terminals beside them.
 */
@Composable
internal fun rememberSessionLabel(sessionManager: TerminalSessionManager): @Composable (TerminalSessionSnapshot, Boolean) -> Unit {
    val revision by rememberTerminalRevision(sessionManager)
    val rows by rememberUpdatedState(rememberSessionLabelRows(sessionManager, revision))
    return remember(sessionManager) {
        { session, selected ->
            val row = rows[session.id]
            if (row != null) SessionTabLabel(row, selected)
            else MoruText(session.title, color = MoruTheme.colors.textSecondary, style = MoruTheme.typography.caption, maxLines = 1)
        }
    }
}
