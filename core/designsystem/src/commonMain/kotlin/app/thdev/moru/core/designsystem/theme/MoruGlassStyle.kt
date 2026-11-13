package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
data class MoruGlassStyle(
    val fill: Color,
    val tint: Color,
    val rim: Color,
    val lowerRim: Color,
    val radius: Dp,
    val elevation: Dp,
)
