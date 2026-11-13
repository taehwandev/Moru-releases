package app.thdev.moru.feature.terminal

import androidx.compose.ui.graphics.Color as ComposeColor
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.model.terminal.TerminalProfile
import app.thdev.moru.core.ui.terminal.moruTerminalColorScheme
import com.jediterm.core.Color as JediColor
import com.jediterm.terminal.ui.JediTermWidget
import java.awt.Color as AwtColor

internal data class TerminalPalette(
    val canvas: JediColor,
    val foreground: JediColor,
    val selection: JediColor,
    val find: JediColor,
    val link: JediColor,
    val ansi: List<JediColor>,
) {
    internal class Holder(
        var palette: TerminalPalette,
    )

    /** Whether the canvas reads as dark, which is what a program asking about the theme is told. */
    val isDark: Boolean
        get() = canvas.red * 299 + canvas.green * 587 + canvas.blue * 114 < 128_000

    fun indexed(index: Int): JediColor {
        return ansi.getOrElse(index.coerceIn(0, 15)) { foreground }
    }

    fun applyTo(widget: JediTermWidget) {
        widget.background = canvas.awt()
        widget.foreground = foreground.awt()
        widget.isOpaque = true
        widget.terminalPanel.apply {
            background = canvas.awt()
            foreground = this@TerminalPalette.foreground.awt()
            isOpaque = true
        }
        widget.component.apply {
            background = canvas.awt()
            foreground = this@TerminalPalette.foreground.awt()
            isOpaque = true
            repaint()
        }
        (widget as? MoruJediTermWidget)?.refreshFindAppearance()
    }

    fun canvasComposeColor(): ComposeColor {
        return ComposeColor(canvas.red, canvas.green, canvas.blue)
    }

    private fun JediColor.awt(): AwtColor {
        return AwtColor(red, green, blue, alpha)
    }

    companion object {
        /** Moru's colors from [colors] with the profile's colors for the current light or dark theme on top. */
        fun from(colors: MoruColors, profile: TerminalProfile?): TerminalPalette {
            val moru = moruTerminalColorScheme(colors)
            val scheme = profile?.colorsFor(light = colors.isLight)?.over(moru) ?: moru
            return from(scheme, find = colors.warning.toJediColor())
        }

        fun from(colors: MoruColors): TerminalPalette = from(colors, null)

        private fun from(scheme: TerminalColorScheme, find: JediColor): TerminalPalette {
            val foreground = scheme.foreground.jedi()
            return TerminalPalette(
                canvas = scheme.background.jedi(),
                foreground = foreground,
                selection = scheme.selection.jedi(),
                find = find,
                link = scheme.link.jedi(),
                ansi = scheme.ansi.map { it.jedi() },
            )
        }

        private fun String?.jedi(): JediColor {
            val rgb = this?.removePrefix("#")?.takeIf { it.length == 6 }?.toIntOrNull(16) ?: 0
            return JediColor((rgb shr 16) and 0xff, (rgb shr 8) and 0xff, rgb and 0xff)
        }

        private fun ComposeColor.toJediColor(): JediColor {
            return JediColor(
                (red * 255).toInt().coerceIn(0, 255),
                (green * 255).toInt().coerceIn(0, 255),
                (blue * 255).toInt().coerceIn(0, 255),
            )
        }
    }
}
