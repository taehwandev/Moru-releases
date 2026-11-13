package app.thdev.moru.core.ui.terminal

import androidx.compose.ui.graphics.Color

/** `#rrggbb` → a Compose color, or null when it is not one. */
fun terminalHexColor(hex: String?): Color? {
    val rgb = hex?.removePrefix("#")?.takeIf { it.length == 6 }?.toIntOrNull(16) ?: return null
    return Color(0xff000000 or rgb.toLong())
}
