package app.thdev.moru.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MoruColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val error: Color,
    val success: Color,
    val warning: Color,
    val isLight: Boolean
)

/**
 * Moru's own light theme, with ink and paper with no hue of their own, so the one accent,
 * the amber of Mo's eyes ([MoruColors.warning]), means "waiting for you" wherever it shows. Amber is darkened here so
 * it stays readable as text on paper.
 */
val lightCkColors = MoruColors(
    background = Color(0xFFFAFAF7),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF1F2026),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF18181B),
    textSecondary = Color(0xFF66666D),
    border = Color(0xFFE6E4DE),
    error = Color(0xFFB42318),
    success = Color(0xFF23794F),
    warning = Color(0xFF8A5A0B),
    isLight = true
)

/** Moru's dark theme: charcoal like Mo's fur, cream for emphasis, and the amber of Mo's eyes for waiting. */
val darkCkColors = MoruColors(
    background = Color(0xFF131316),
    surface = Color(0xFF1B1B1F),
    primary = Color(0xFFF2F1EC),
    onPrimary = Color(0xFF141414),
    textPrimary = Color(0xFFECEBE6),
    textSecondary = Color(0xFF9C9CA3),
    border = Color(0xFF2A2A30),
    error = Color(0xFFFF8A80),
    success = Color(0xFF4CC08C),
    warning = Color(0xFFF6B93B),
    isLight = false
)

val freshMintCkColors = MoruColors(
    background = Color(0xFFF4FBFA),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF007575),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF102A43),
    textSecondary = Color(0xFF5B7184),
    border = Color(0xFFD6E7E8),
    error = Color(0xFFC42020),
    success = Color(0xFF187A3A),
    warning = Color(0xFF925600),
    isLight = true,
)

val githubLightCkColors = MoruColors(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF0969DA),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF24292F),
    textSecondary = Color(0xFF57606A),
    border = Color(0xFFD0D7DE),
    error = Color(0xFFC11F2B),
    success = Color(0xFF18732F),
    warning = Color(0xFF895C00),
    isLight = true,
)

/** GitHub's neutral dark surfaces with an accent readable as both text and a button fill. */
val githubDarkCkColors = MoruColors(
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF0D1117),
    textPrimary = Color(0xFFC9D1D9),
    textSecondary = Color(0xFF8B949E),
    border = Color(0xFF30363D),
    error = Color(0xFFFF7B72),
    success = Color(0xFF3FB950),
    warning = Color(0xFFD29922),
    isLight = false,
)

/** Latte hues adapted for small UI text on both Base and Mantle surfaces. */
val catppuccinLatteCkColors = MoruColors(
    background = Color(0xFFEFF1F5),
    surface = Color(0xFFE6E9EF),
    primary = Color(0xFF1957D2),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF4C4F69),
    textSecondary = Color(0xFF5C5F77),
    border = Color(0xFFCCD0DA),
    error = Color(0xFFB90D32),
    success = Color(0xFF246A19),
    warning = Color(0xFF895700),
    isLight = true,
)

val catppuccinMochaCkColors = MoruColors(
    background = Color(0xFF1E1E2E),
    surface = Color(0xFF181825),
    primary = Color(0xFF89B4FA),
    onPrimary = Color(0xFF11111B),
    textPrimary = Color(0xFFCDD6F4),
    textSecondary = Color(0xFFBAC2DE),
    border = Color(0xFF45475A),
    error = Color(0xFFF38BA8),
    success = Color(0xFFA6E3A1),
    warning = Color(0xFFF9E2AF),
    isLight = false,
)

val tokyoNightCkColors = MoruColors(
    background = Color(0xFF1A1B26),
    surface = Color(0xFF16161E),
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF111827),
    textPrimary = Color(0xFFC0CAF5),
    textSecondary = Color(0xFFA9B1D6),
    border = Color(0xFF292E42),
    error = Color(0xFFF7768E),
    success = Color(0xFF9ECE6A),
    warning = Color(0xFFE0AF68),
    isLight = false,
)

val LocalCkColors = staticCompositionLocalOf { lightCkColors }
