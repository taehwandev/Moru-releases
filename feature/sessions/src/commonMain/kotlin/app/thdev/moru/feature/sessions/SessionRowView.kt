package app.thdev.moru.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruBadge
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * Provider and state identify a session first; its work gets an uninterrupted line below.
 * Questions, current output, ownership, and actions remain visible beneath the work.
 */
@Composable
fun SessionRowView(
    row: AgentSessionRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    indent: Dp = 0.dp,
    context: String? = null,
    accent: MoruIdentityAccent? = null,
    onOpenPreview: (sessionId: String, url: String) -> Unit = { _, _ -> },
    actions: (@Composable () -> Unit)? = null,
) {
    val strings = sessionsStrings()
    val agentName = strings.agentName(row.agent)
    val permissionRequests by SessionPermissions.requests.collectAsState()
    val state = sessionRowState(row, strings, row.sessionId in permissionRequests)
    val headline = sessionTabHeadline(row, agentName)
    val headlineIsQuestion = (row.activity == SessionActivity.Asking && sessionTextMatches(row.question, headline)) ||
        (row.awaitsReply && sessionTextMatches(row.replyQuestion, headline))
    val activityLabel = listOfNotNull(state.label, sessionRowHint(row, strings)).joinToString(". ")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoruTheme.radius.small))
            .ckSurface(if (row.selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
            .sessionRowEmphasis(state.emphasis, row.selected, accent?.color(MoruTheme.colors) ?: MoruTheme.colors.primary)
            .testTag("session-row:${row.sessionId}")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                selected = row.selected
                contentDescription = strings.rowDescription(agentName, context?.let { "$headline, $it" } ?: headline, activityLabel)
            }
            .padding(start = indent + 7.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            AgentMark(row.agent, Modifier.testTag("session-agent:${row.sessionId}"), size = 12.dp)
            MoruText(
                text = agentName,
                modifier = Modifier.weight(1f),
                style = MoruTheme.typography.micro.copy(fontWeight = FontWeight.Medium),
                color = MoruTheme.colors.textSecondary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MoruBadge(state.label, Modifier.testTag("session-status:${row.sessionId}"), state.badgeType)
            actions?.invoke()
        }
        MoruText(
            text = headline,
            modifier = Modifier.testTag("session-headline:${row.sessionId}"),
            style = MoruTheme.typography.caption.copy(
                fontWeight = if (row.selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = when {
                headlineIsQuestion -> MoruTheme.colors.warning
                row.selected -> MoruTheme.colors.textPrimary
                else -> MoruTheme.colors.textPrimary.copy(alpha = 0.9f)
            },
            maxLines = if (headlineIsQuestion) 3 else 1,
            overflow = TextOverflow.Ellipsis,
        )
        SessionRowDetails(row, context, onOpenPreview)
    }
}
