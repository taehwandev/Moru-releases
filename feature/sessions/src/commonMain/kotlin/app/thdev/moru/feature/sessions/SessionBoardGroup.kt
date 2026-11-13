package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * Only occupied status sections take space; each owns its Project and Task hierarchy. Turns waiting for the user's
 * reply sit on an amber card and finished responses on a green one, so they stand out among the running sessions.
 */
@Composable
internal fun SessionBoardGroup(
    section: SessionBoardSection,
    label: String,
    glyph: MoruStatusGlyphKind,
    rows: List<AgentSessionRow>,
    tasks: Map<String, SessionTaskRef>,
    onSelectSession: (AgentSessionRow, String?) -> Unit,
    onOpenPreview: (String, String) -> Unit,
) {
    if (rows.isEmpty()) return
    val tint = when (section) {
        SessionBoardSection.AwaitingReply -> MoruTheme.colors.warning
        SessionBoardSection.Completed -> MoruTheme.colors.success
        else -> null
    }
    val shape = RoundedCornerShape(MoruTheme.radius.medium)
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .then(if (tint != null) Modifier.padding(horizontal = 6.dp).clip(shape).background(tint.copy(alpha = 0.08f))
                .border(1.dp, tint.copy(alpha = 0.35f), shape).padding(bottom = 8.dp) else Modifier)
            .testTag("session-board-section:$section"),
    ) {
        SessionBoardHeading(label, rows.size, glyph, color = tint ?: MoruTheme.colors.textPrimary)
        SessionBoardCards(rows, tasks, onSelectSession, onOpenPreview)
    }
}
