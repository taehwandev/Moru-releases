package app.thdev.moru.core.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruIdentityAccent
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Quiet grouping and a leading rail; caller-owned labels and status remain readable independently. */
@Composable
fun Modifier.moruIdentitySurface(accent: MoruIdentityAccent, role: MoruIdentitySurfaceRole): Modifier {
    val colors = MoruTheme.colors
    val color = accent.color(colors)
    val group = role == MoruIdentitySurfaceRole.Group || role == MoruIdentitySurfaceRole.GroupSelected
    val selected = role == MoruIdentitySurfaceRole.GroupSelected || role == MoruIdentitySurfaceRole.RowSelected
    val shape = RoundedCornerShape(if (group) MoruTheme.radius.medium else MoruTheme.radius.small)
    val fill = when (role) {
        MoruIdentitySurfaceRole.Group -> if (colors.isLight) Color(0xFFF3F4F6) else Color(0xFF16181D)
        MoruIdentitySurfaceRole.GroupSelected -> if (colors.isLight) color.copy(alpha = 0.07f) else Color(0xFF1A1D24)
        MoruIdentitySurfaceRole.Row -> Color.Transparent
        MoruIdentitySurfaceRole.RowSelected -> color.copy(alpha = if (colors.isLight) 0.14f else 0.22f)
    }
    val stroke = MoruTheme.stroke.thin
    val rail = 3.dp
    val borderColor = when (role) {
        MoruIdentitySurfaceRole.GroupSelected -> color.copy(alpha = if (colors.isLight) 0.50f else 0.40f)
        MoruIdentitySurfaceRole.Group -> colors.border.copy(alpha = if (colors.isLight) 0.70f else 0.50f)
        // A selected row inside a group shows by its fill; the group's outline is the only one.
        MoruIdentitySurfaceRole.Row, MoruIdentitySurfaceRole.RowSelected -> Color.Transparent
    }
    return clip(shape)
        .background(fill)
        .then(if (group) Modifier.border(stroke, borderColor, shape) else Modifier)
        .drawWithContent {
            drawContent()
            if (group) {
                val width = rail.toPx()
                drawLine(
                    color = color.copy(alpha = if (selected) 1f else 0.75f),
                    start = Offset(width / 2, 0f),
                    end = Offset(width / 2, size.height),
                    strokeWidth = width,
                )
            }
        }
}

