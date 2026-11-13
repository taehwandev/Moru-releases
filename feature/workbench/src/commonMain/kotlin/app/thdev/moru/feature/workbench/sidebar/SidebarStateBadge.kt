package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * A tinted pill for work the user has not looked at yet, on a Project header (with a count) or under a Task title:
 * green "✓ Done" for a finished agent turn, amber "◆ Your reply" for a turn that ended with a question. Neither says
 * the Task itself is closed.
 */
@Composable
internal fun SidebarStateBadge(text: String, glyph: MoruStatusGlyphKind, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruStatusGlyph(kind = glyph, size = 10.dp)
        MoruText(text, style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), color = color, maxLines = 1)
    }
}
