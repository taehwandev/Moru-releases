package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.feature.sessions.i18n.sessionsStrings

/**
 * A terminal tab's or pane header's label: its status mark, its agent's mark and what the agent is working on. The
 * state's word and the project name stay in the sidebar row; here the mark says the state. Emitted into the caller's row.
 */
@Composable
fun SessionTabLabel(row: AgentSessionRow, selected: Boolean) {
    val strings = sessionsStrings()
    val state = sessionRowState(row, strings)
    val headline = sessionTabHeadline(row, strings.agentName(row.agent))
    MoruStatusGlyph(kind = state.glyph, size = 11.dp)
    AgentMark(row.agent, size = 14.dp)
    MoruText(
        text = headline,
        modifier = Modifier.widthIn(max = 220.dp),
        color = if (selected) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
        style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
