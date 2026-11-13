package app.thdev.moru.feature.git.history

import androidx.compose.ui.graphics.Color

private val DarkLaneColors = listOf(
    Color(0xFF4FC3F7),
    Color(0xFF81C784),
    Color(0xFFFFB74D),
    Color(0xFFBA68C8),
    Color(0xFFF06292),
    Color(0xFF4DB6AC),
    Color(0xFFFFD54F),
    Color(0xFF7986CB),
)

private val LightLaneColors = listOf(
    Color(0xFF0288D1),
    Color(0xFF388E3C),
    Color(0xFFE65100),
    Color(0xFF7B1FA2),
    Color(0xFFC2185B),
    Color(0xFF00796B),
    Color(0xFFB28704),
    Color(0xFF3949AB),
)

/**
 * The lane colors for a theme, [GIT_GRAPH_COLOR_COUNT] of them, indexed by the palette
 * index in [GitGraphRow]. Lighter tones on dark backgrounds and deeper tones on light
 * ones keep every lane readable, and no lane color is reserved for status.
 */
fun gitGraphLaneColors(isLight: Boolean): List<Color> = if (isLight) LightLaneColors else DarkLaneColors
