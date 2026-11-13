package app.thdev.moru.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.scroll.stableScrollAnchor
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/** One session under its Task heading; choosing it preserves direct session selection. */
@Composable
internal fun ProjectAgentActivityLine(row: AgentSessionRow, taskTitle: String, onClick: () -> Unit) {
    val strings = sessionsStrings()
    val agentName = strings.agentName(row.agent)
    val permissionRequests by SessionPermissions.requests.collectAsState()
    val state = sessionRowState(row, strings, row.sessionId in permissionRequests)
    val headline = sessionTabHeadline(row, agentName)
    val headlineHeight = with(LocalDensity.current) { MoruTheme.typography.caption.lineHeight.toDp() * 2 }
    Row(
        Modifier.fillMaxWidth()
            .stableScrollAnchor("session:${row.sessionId}")
            .clip(RoundedCornerShape(MoruTheme.radius.small))
            .ckSurface(if (row.selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
            .sessionRowEmphasis(state.emphasis, row.selected, MoruTheme.colors.primary)
            .testTag("project-agent-line:${row.sessionId}")
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                role = Role.Button
                selected = row.selected
                contentDescription = strings.rowDescription(agentName, "$taskTitle, $headline", state.label)
                onClick { onClick(); true }
            }
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruStatusGlyph(state.glyph, size = 10.dp)
        MoruText(headline, Modifier.weight(1f).heightIn(min = headlineHeight), style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        AgentMark(row.agent, Modifier.testTag("project-agent-mark:${row.sessionId}"), size = 12.dp)
        MoruText(state.label, style = MoruTheme.typography.micro, maxLines = 1,
            color = if (state.glyph == MoruStatusGlyphKind.NeedsYou) {
                MoruTheme.colors.warning
            } else MoruTheme.colors.textSecondary)
    }
}
