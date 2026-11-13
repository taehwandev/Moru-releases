package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

private const val ARC_SWEEP_DEGREES = 270f
private const val STROKE_RATIO = 0.14f

/**
 * One workbench status mark. The shape carries the meaning ([MoruStatusGlyphKind]); the colour only
 * reinforces it. Every mark is still: a sidebar of sessions that each spun looked like the app was
 * forever loading, and it kept the render clock running while nothing changed.
 * Pass [contentDescription] where the glyph stands alone without a text label next to it.
 */
@Composable
fun MoruStatusGlyph(
    kind: MoruStatusGlyphKind,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    contentDescription: String? = null,
) {
    val colors = MoruTheme.colors
    val describe = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(modifier = modifier.size(size).then(describe)) {
        when (kind) {
            MoruStatusGlyphKind.Working -> drawWorking(colors.success)
            MoruStatusGlyphKind.NeedsYou -> drawDiamond(colors.warning)
            MoruStatusGlyphKind.Unread -> drawDot(colors.primary)
            MoruStatusGlyphKind.Done -> drawCheck(colors.success)
            MoruStatusGlyphKind.Error -> drawCross(colors.error)
            MoruStatusGlyphKind.Idle -> drawRing(colors.textSecondary)
        }
    }
}

private fun DrawScope.strokeWidth(): Float = size.minDimension * STROKE_RATIO

private fun DrawScope.drawWorking(color: Color) {
    val width = strokeWidth()
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = ARC_SWEEP_DEGREES,
        useCenter = false,
        topLeft = Offset(width / 2, width / 2),
        size = Size(size.width - width, size.height - width),
        style = Stroke(width = width, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRing(color: Color) {
    val width = strokeWidth()
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(width / 2, width / 2),
        size = Size(size.width - width, size.height - width),
        style = Stroke(width = width),
    )
}

private fun DrawScope.drawDot(color: Color) {
    drawCircle(color = color, radius = size.minDimension * 0.3f)
}

private fun DrawScope.drawDiamond(color: Color) {
    val side = size.minDimension
    val path = Path().apply {
        moveTo(side * 0.5f, side * 0.04f)
        lineTo(side * 0.96f, side * 0.5f)
        lineTo(side * 0.5f, side * 0.96f)
        lineTo(side * 0.04f, side * 0.5f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawCheck(color: Color) {
    val side = size.minDimension
    val path = Path().apply {
        moveTo(side * 0.16f, side * 0.54f)
        lineTo(side * 0.40f, side * 0.76f)
        lineTo(side * 0.84f, side * 0.26f)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth() * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawCross(color: Color) {
    val side = size.minDimension
    val width = strokeWidth() * 1.2f
    drawLine(color, Offset(side * 0.2f, side * 0.2f), Offset(side * 0.8f, side * 0.8f), width, StrokeCap.Round)
    drawLine(color, Offset(side * 0.8f, side * 0.2f), Offset(side * 0.2f, side * 0.8f), width, StrokeCap.Round)
}
