package app.thdev.moru.core.ui.terminal

import androidx.compose.ui.graphics.Color
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.model.terminal.TerminalColorScheme

/** Moru's own terminal colors for [colors], every slot set; a user's or imported scheme overrides them slot by slot. */
fun moruTerminalColorScheme(colors: MoruColors): TerminalColorScheme {
    val foreground = colors.textPrimary
    val cyan = blend(colors.primary, colors.success, 0.48f)
    val magenta = blend(colors.primary, colors.error, 0.42f)
    val muted = blend(colors.background, colors.textSecondary, if (colors.isLight) 0.55f else 0.70f)
    val brightForeground = if (colors.isLight) colors.textPrimary else Color.White
    val ansi = listOf(
        colors.background, colors.error, colors.success, colors.warning, colors.primary, magenta, cyan, foreground,
        muted,
        blend(colors.error, brightForeground, 0.18f),
        blend(colors.success, brightForeground, 0.18f),
        blend(colors.warning, brightForeground, 0.18f),
        blend(colors.primary, brightForeground, 0.18f),
        blend(magenta.rounded(), brightForeground, 0.18f),
        blend(cyan.rounded(), brightForeground, 0.18f),
        brightForeground,
    )
    return TerminalColorScheme(
        foreground = foreground.terminalHex(),
        background = colors.background.terminalHex(),
        selection = blend(colors.background, colors.primary, if (colors.isLight) 0.18f else 0.32f).terminalHex(),
        link = colors.primary.terminalHex(),
        ansi = ansi.map { it.terminalHex() },
    )
}

private fun blend(base: Color, overlay: Color, overlayAlpha: Float): Color {
    val alpha = overlayAlpha.coerceIn(0f, 1f)
    val inverse = 1f - alpha
    return Color(
        red = base.red * inverse + overlay.red * alpha,
        green = base.green * inverse + overlay.green * alpha,
        blue = base.blue * inverse + overlay.blue * alpha,
        alpha = 1f,
    )
}

/** The color as the terminal draws it, each channel cut to a whole 0–255 step. */
private fun Color.rounded(): Color = Color(terminalChannel(red), terminalChannel(green), terminalChannel(blue))
