package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily

/** The installed family called [name], or the default family when none is chosen. */
expect fun systemFontFamily(name: String?): FontFamily
