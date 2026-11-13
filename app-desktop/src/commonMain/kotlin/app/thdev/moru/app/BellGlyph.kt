package app.thdev.moru.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** The notification bell outline, drawn to fill the current canvas. */
internal fun DrawScope.drawBell(color: Color, stroke: Stroke) {
    val bell = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.70f)
        lineTo(size.width * 0.29f, size.height * 0.58f)
        lineTo(size.width * 0.29f, size.height * 0.40f)
        cubicTo(
            size.width * 0.29f, size.height * 0.13f,
            size.width * 0.71f, size.height * 0.13f,
            size.width * 0.71f, size.height * 0.40f,
        )
        lineTo(size.width * 0.71f, size.height * 0.58f)
        lineTo(size.width * 0.78f, size.height * 0.70f)
        close()
    }
    drawPath(bell, color, style = stroke)
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.40f, size.height * 0.73f),
        size = Size(size.width * 0.20f, size.height * 0.13f),
        style = stroke,
    )
}
