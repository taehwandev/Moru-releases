package app.thdev.moru.core.ui.terminal

import androidx.compose.ui.graphics.Color

/** The color as `#rrggbb`, each channel cut to a whole 0–255 step as the terminal draws it. */
fun Color.terminalHex(): String =
    "#" + listOf(red, green, blue).joinToString("") { terminalChannel(it).toString(16).padStart(2, '0') }

internal fun terminalChannel(value: Float): Int = (value * 255).toInt().coerceIn(0, 255)
