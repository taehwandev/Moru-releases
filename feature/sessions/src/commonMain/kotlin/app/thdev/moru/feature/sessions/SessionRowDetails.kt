package app.thdev.moru.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.ui.i18n.coreUiStrings
import app.thdev.moru.core.ui.time.formatRelativeTime
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** Secondary evidence and actions stay readable below the compact session line. */
@Composable
internal fun SessionRowDetails(row: AgentSessionRow, context: String?, onOpenPreview: (String, String) -> Unit) {
    val strings = sessionsStrings()
    val headline = sessionTabHeadline(row, strings.agentName(row.agent))
    val detail = when {
        row.activity == SessionActivity.Asking -> row.question
        row.awaitsReply -> row.replyQuestion
        row.activity == SessionActivity.Failed -> row.exitCode?.takeIf { it != 0 }?.let(strings::exitCode)
        row.activity == SessionActivity.Working && row.summary != null -> row.lastLine
        else -> null
    }?.takeIf(String::isNotBlank)?.takeUnless { sessionTextMatches(it, headline) }
    detail?.let {
        MoruText(it, style = MoruTheme.typography.caption,
            color = when {
                row.activity == SessionActivity.Asking -> MoruTheme.colors.warning
                row.awaitsReply -> MoruTheme.colors.warning
                else -> MoruTheme.colors.textSecondary
            },
            maxLines = if (row.activity == SessionActivity.Asking || row.awaitsReply) 3 else 1, overflow = TextOverflow.Ellipsis)
    }
    val lastOutput = row.lastOutputAtMillis?.let {
        formatRelativeTime(coreUiStrings(), row.nowMillis / 1000, it / 1000)
    }
    if (context != null || lastOutput != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            context?.let {
                MoruText(it, modifier = Modifier.weight(1f), style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            lastOutput?.let {
                MoruText(it, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
            }
        }
    }
    row.previewUrl?.let { url ->
        MoruText(strings.openPreview(url.substringAfter("://").trimEnd('/')),
            Modifier.clickable(role = Role.Button) { onOpenPreview(row.sessionId, url) },
            style = MoruTheme.typography.caption, color = MoruTheme.colors.primary, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
    SessionPermissionActions(row.sessionId)
}
