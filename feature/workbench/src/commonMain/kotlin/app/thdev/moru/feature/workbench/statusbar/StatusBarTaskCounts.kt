package app.thdev.moru.feature.workbench.statusbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** How many Tasks wait for the user and how many are running, each beside its status mark. */
@Composable
internal fun StatusBarTaskCounts(
    waiting: Int,
    working: Int,
) {
    val strings = workbenchStrings()
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (waiting > 0) {
            Count(MoruStatusGlyphKind.NeedsYou, strings.waiting(waiting), MoruTheme.colors.warning, bold = true)
        }
        if (working > 0) {
            Count(MoruStatusGlyphKind.Working, strings.working(working), MoruTheme.colors.textSecondary, bold = false)
        }
        if (waiting == 0 && working == 0) {
            MoruText(text = strings.noActiveTasks, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Count(
    kind: MoruStatusGlyphKind,
    text: String,
    color: Color,
    bold: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        MoruStatusGlyph(kind = kind, size = 10.dp)
        MoruText(
            text = text,
            style = MoruTheme.typography.caption.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
            color = color,
            maxLines = 1,
        )
    }
}
