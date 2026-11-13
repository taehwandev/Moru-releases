package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** A quiet status label. Its shape remains meaningful when space permits only the mark. */
@Composable
fun MoruStatusLabel(
    text: String,
    kind: MoruStatusGlyphKind,
    modifier: Modifier = Modifier,
    showText: Boolean = true,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MoruStatusGlyph(kind, contentDescription = text.takeUnless { showText })
        if (showText) {
            MoruText(text, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
