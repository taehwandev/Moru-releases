package app.thdev.moru.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/** Scales the IDE's typography independently from the PTY renderer. */
fun appTypography(fontFamily: String?, fontSize: Int): MoruTypography {
    val family = systemFontFamily(fontFamily)
    val scale = fontSize.coerceIn(10..28) / 14f
    fun TextStyle.scaled(defaultFamily: FontFamily = family) = copy(
        fontFamily = defaultFamily,
        fontSize = this.fontSize * scale,
        lineHeight = lineHeight * scale,
    )
    return defaultCkTypography.run {
        MoruTypography(
            h1 = h1.scaled(), h2 = h2.scaled(), title = title.scaled(), body = body.scaled(), caption = caption.scaled(),
            label = label.scaled(), micro = micro.scaled(), metric = metric.scaled(), display = display.scaled(), headline = headline.scaled(),
            monospace = monospace.scaled(if (fontFamily.isNullOrBlank()) FontFamily.Monospace else family),
        )
    }
}
