package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalTextApi::class)
actual fun systemFontFamily(name: String?): FontFamily =
    name?.takeIf { it.isNotBlank() }?.let { FontFamily(it) } ?: FontFamily.Default
