package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun MoruGlassRole.resolveGlassStyle(colors: MoruColors): MoruGlassStyle {
    val selected = this == MoruGlassRole.SelectedCard
    return MoruGlassStyle(
        fill = colors.surface.copy(alpha = if (colors.isLight) 0.82f else 0.92f),
        tint = colors.primary.copy(alpha = if (selected) 0.10f else if (colors.isLight) 0.025f else 0.045f),
        rim = Color.White.copy(alpha = if (colors.isLight) 0.90f else 0.24f),
        lowerRim = if (selected) colors.primary.copy(alpha = 0.28f) else colors.border.copy(alpha = 0.65f),
        radius = when (this) {
            MoruGlassRole.Navigation -> 0.dp
            MoruGlassRole.Control -> 10.dp
            MoruGlassRole.Sheet -> 20.dp
            else -> 16.dp
        },
        elevation = when (this) {
            MoruGlassRole.Navigation, MoruGlassRole.Control -> 0.dp
            MoruGlassRole.Sheet -> 18.dp
            else -> 3.dp
        },
    )
}
