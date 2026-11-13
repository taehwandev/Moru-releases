package app.thdev.moru.feature.sessions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Selection stays visible independently of activity, including faded idle, waiting, and ended rows. */
internal fun Modifier.sessionRowEmphasis(emphasis: SessionRowEmphasis, selected: Boolean, bar: Color): Modifier = when (emphasis) {
    SessionRowEmphasis.Active -> drawInsetBar(bar)
    SessionRowEmphasis.Faded -> if (selected) {
        drawInsetBar(bar)
    } else alpha(0.64f)
    SessionRowEmphasis.Plain -> if (selected) drawInsetBar(bar) else this
}

private fun Modifier.drawInsetBar(bar: Color): Modifier = drawBehind {
    val barWidth = 2.5.dp.toPx()
    val vInset = 5.dp.toPx()
    val barHeight = (size.height - vInset * 2f).coerceAtLeast(4.dp.toPx())
    drawRoundRect(
        color = bar,
        topLeft = Offset(1.dp.toPx(), (size.height - barHeight) / 2f),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
    )
}
