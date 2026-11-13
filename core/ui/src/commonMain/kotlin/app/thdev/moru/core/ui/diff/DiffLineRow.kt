package app.thdev.moru.core.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
private const val TINT_ALPHA = 0.16f

/**
 * One diff line with both line-number gutters and a +/- marker, so a change reads without
 * its tint. The text wraps rather than scrolls sideways, which keeps a long line readable.
 */
@Composable
internal fun DiffLineRow(line: DiffLine, modifier: Modifier = Modifier) {
    val colors = MoruTheme.colors
    val tint = when (line.kind) {
        DiffLineKind.Added -> colors.success.copy(alpha = TINT_ALPHA)
        DiffLineKind.Removed -> colors.error.copy(alpha = TINT_ALPHA)
        DiffLineKind.Context -> Color.Transparent
    }
    val marker = when (line.kind) {
        DiffLineKind.Added -> "+"
        DiffLineKind.Removed -> "−"
        DiffLineKind.Context -> " "
    }
    val style = MoruTheme.typography.monospace
    Row(
        modifier = modifier.fillMaxWidth().background(tint).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Gutter(line.oldNumber)
        Gutter(line.newNumber)
        MoruText(
            text = marker,
            style = style.copy(textAlign = TextAlign.Center),
            modifier = Modifier.width(16.dp),
        )
        MoruText(text = line.text.ifEmpty { " " }, style = style, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Gutter(number: Int?) {
    MoruText(
        text = number?.toString().orEmpty(),
        style = MoruTheme.typography.monospace.copy(textAlign = TextAlign.End),
        color = MoruTheme.colors.textSecondary,
        modifier = Modifier.width(44.dp).padding(end = 8.dp),
    )
}
