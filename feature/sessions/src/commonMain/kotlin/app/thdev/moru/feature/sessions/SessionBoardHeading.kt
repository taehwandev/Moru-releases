package app.thdev.moru.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import androidx.compose.ui.graphics.Color

/** A status section's title and count lead the ownership hierarchy below it. */
@Composable
internal fun SessionBoardHeading(label: String, count: Int, glyph: MoruStatusGlyphKind, color: Color = MoruTheme.colors.textPrimary) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruStatusGlyph(glyph)
        MoruText(label, Modifier.weight(1f),
            style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MoruText(count.toString(), style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}
