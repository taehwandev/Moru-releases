package app.thdev.moru.app

import androidx.compose.ui.graphics.drawscope.rotate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** One activity-bar rail entry: selection indicator plus the destination icon. */
@Composable
internal fun RailItem(
    destination: MoruDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MoruTheme.colors
    val label = appStrings().destinationLabel(destination)
    val selectedBackground = colors.primary.copy(alpha = if (colors.isLight) 0.10f else 0.18f)
    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(MoruTheme.radius.medium))
            .semantics { contentDescription = label }
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .background(if (selected) selectedBackground else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(if (selected) colors.primary else Color.Transparent),
        )
        val iconColor = if (selected) colors.textPrimary else colors.textSecondary
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                when (destination) {
                    MoruDestination.Workbench -> {
                        val path = Path().apply {
                            moveTo(size.width * 0.12f, size.height * 0.32f)
                            lineTo(size.width * 0.37f, size.height * 0.32f)
                            lineTo(size.width * 0.45f, size.height * 0.42f)
                            lineTo(size.width * 0.88f, size.height * 0.42f)
                            lineTo(size.width * 0.88f, size.height * 0.80f)
                            lineTo(size.width * 0.12f, size.height * 0.80f)
                            close()
                        }
                        drawPath(path = path, color = iconColor, style = stroke)
                    }
                    MoruDestination.Knowledge -> {
                        val top = Offset(size.width * 0.5f, size.height * 0.22f)
                        val left = Offset(size.width * 0.25f, size.height * 0.74f)
                        val right = Offset(size.width * 0.76f, size.height * 0.74f)
                        drawLine(iconColor, top, left, stroke.width, StrokeCap.Round)
                        drawLine(iconColor, top, right, stroke.width, StrokeCap.Round)
                        drawLine(iconColor, left, right, stroke.width, StrokeCap.Round)
                        listOf(top, left, right).forEach { c ->
                            drawCircle(iconColor, size.minDimension * 0.105f, c, style = stroke)
                        }
                    }
                    MoruDestination.Notifications -> drawBell(iconColor, stroke)
                    MoruDestination.Settings -> {
                        val center = Offset(size.width * 0.5f, size.height * 0.5f)
                        val outer = size.minDimension * 0.34f
                        val tick = size.minDimension * 0.12f
                        drawCircle(iconColor, outer, center, style = Stroke(width = stroke.width))
                        drawCircle(iconColor, size.minDimension * 0.16f, center, style = Stroke(width = stroke.width))
                        repeat(8) { index ->
                            val angle = ((index * 45.0) - 90.0) * kotlin.math.PI / 180.0
                            val cos = kotlin.math.cos(angle).toFloat()
                            val sin = kotlin.math.sin(angle).toFloat()
                            val start = Offset(center.x + cos * (outer + tick * 0.3f), center.y + sin * (outer + tick * 0.3f))
                            val end = Offset(center.x + cos * (outer + tick), center.y + sin * (outer + tick))
                            drawLine(iconColor, start, end, stroke.width, StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}
