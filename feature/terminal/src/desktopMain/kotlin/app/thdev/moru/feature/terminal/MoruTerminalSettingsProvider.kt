package app.thdev.moru.feature.terminal

import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.core.model.terminal.TerminalProfile
import com.jediterm.core.Color as JediColor
import com.jediterm.terminal.CursorShape
import com.jediterm.terminal.HyperlinkStyle
import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.emulator.ColorPalette
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Font
import java.awt.GraphicsEnvironment

internal class MoruTerminalSettingsProvider(
    fontFamily: String?,
    fontSize: Int,
    private val scrollbackLines: Int,
    private val paletteHolder: TerminalPalette.Holder,
    profile: TerminalProfile? = null,
    /** Option (Alt) sends Escape before the key; read on every key press, so a change applies at once. */
    @Volatile var optionSendsEscape: Boolean = true,
) : DefaultSettingsProvider() {
    /** The imported profile's spacing and cursor; null keeps Moru's. */
    var profile: TerminalProfile? = profile
        private set

    private var requestedFontFamily = fontFamily
    private var resolvedFontFamily = resolveTerminalFontFamily(fontFamily)
    private var resolvedFontSize = fontSize.coerceIn(MinTerminalFontSize, MaxTerminalFontSize)

    /** Takes the font from the current settings; true when the terminal must lay its text out again. */
    fun updateFont(fontFamily: String?, fontSize: Int): Boolean {
        val size = fontSize.coerceIn(MinTerminalFontSize, MaxTerminalFontSize)
        if (fontFamily == requestedFontFamily && size == resolvedFontSize) return false
        if (fontFamily != requestedFontFamily) {
            requestedFontFamily = fontFamily
            resolvedFontFamily = resolveTerminalFontFamily(fontFamily)
        }
        resolvedFontSize = size
        return true
    }

    /** Takes a new imported profile; true when the terminal must lay its text out again for a new line spacing. */
    fun updateProfile(next: TerminalProfile?): Boolean {
        if (next == profile) return false
        val relayout = next?.lineSpacing != profile?.lineSpacing
        profile = next
        return relayout
    }

    /** The cursor a program gets until it asks for another; Moru's own is a blinking block. */
    fun cursorShape(): CursorShape {
        val blink = profile?.cursorBlink ?: (profile?.cursor == null)
        return when (profile?.cursor ?: TerminalCursorStyle.Block) {
            TerminalCursorStyle.Block -> if (blink) CursorShape.BLINK_BLOCK else CursorShape.STEADY_BLOCK
            TerminalCursorStyle.Underline -> if (blink) CursorShape.BLINK_UNDERLINE else CursorShape.STEADY_UNDERLINE
            TerminalCursorStyle.Bar -> if (blink) CursorShape.BLINK_VERTICAL_BAR else CursorShape.STEADY_VERTICAL_BAR
        }
    }

    override fun getDefaultForeground(): TerminalColor {
        return paletteHolder.palette.foreground.terminal()
    }

    override fun getDefaultBackground(): TerminalColor {
        return paletteHolder.palette.canvas.terminal()
    }

    override fun getTerminalColorPalette(): ColorPalette {
        return MoruTerminalColorPalette(paletteHolder)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getDefaultStyle(): TextStyle {
        return TextStyle(getDefaultForeground(), getDefaultBackground())
    }

    override fun getTerminalFont(): Font {
        return Font(resolvedFontFamily, Font.PLAIN, resolvedFontSize)
    }

    override fun getTerminalFontSize(): Float {
        return resolvedFontSize.toFloat()
    }

    override fun getLineSpacing(): Float {
        return profile?.lineSpacing ?: DefaultLineSpacing
    }

    override fun shouldFillCharacterBackgroundIncludingLineSpacing(): Boolean {
        return true
    }

    override fun getSelectionColor(): TextStyle {
        return TextStyle(paletteHolder.palette.foreground.terminal(), paletteHolder.palette.selection.terminal())
    }

    override fun getFoundPatternColor(): TextStyle {
        return TextStyle(paletteHolder.palette.canvas.terminal(), paletteHolder.palette.find.terminal())
    }

    override fun getHyperlinkColor(): TextStyle {
        return TextStyle(paletteHolder.palette.link.terminal(), null)
    }

    override fun getHyperlinkHighlightingMode(): HyperlinkStyle.HighlightMode {
        return HyperlinkStyle.HighlightMode.HOVER
    }

    override fun getBufferMaxLinesCount(): Int {
        return scrollbackLines.coerceIn(MinScrollbackLines, MaxScrollbackLines)
    }

    override fun copyOnSelect(): Boolean {
        return false
    }

    override fun scrollToBottomOnTyping(): Boolean {
        return true
    }

    override fun audibleBell(): Boolean {
        return false
    }

    override fun useAntialiasing(): Boolean {
        return true
    }

    override fun altSendsEscape(): Boolean {
        return optionSendsEscape
    }

    override fun enableMouseReporting(): Boolean {
        return true
    }

    private fun JediColor.terminal(): TerminalColor {
        return TerminalColor.rgb(red, green, blue)
    }
}

private class MoruTerminalColorPalette(
    private val paletteHolder: TerminalPalette.Holder,
) : ColorPalette() {
    override fun getForeground(color: TerminalColor): JediColor {
        return color.resolve(paletteHolder.palette.foreground) { index ->
            paletteHolder.palette.indexed(index)
        }
    }

    override fun getBackground(color: TerminalColor): JediColor {
        return color.resolve(paletteHolder.palette.canvas) { index ->
            paletteHolder.palette.indexed(index)
        }
    }

    public override fun getForegroundByColorIndex(colorIndex: Int): JediColor {
        return paletteHolder.palette.indexed(colorIndex)
    }

    override fun getBackgroundByColorIndex(colorIndex: Int): JediColor {
        return paletteHolder.palette.indexed(colorIndex)
    }
}

private fun TerminalColor.resolve(
    fallback: JediColor,
    ansiColor: (Int) -> JediColor,
): JediColor {
    if (!isIndexed) return toColor()

    val index = colorIndex
    if (index in 0..15) return ansiColor(index)
    if (index !in 16..255) return fallback

    return ColorPalette
        .getIndexedTerminalColor(index)
        ?.toColor()
        ?: fallback
}

private fun resolveTerminalFontFamily(preferredFamily: String?): String {
    val availableFamilies = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .availableFontFamilyNames
        .associateBy { family -> family.lowercase() }

    val candidates = listOfNotNull(preferredFamily?.takeIf { family -> family.isNotBlank() }) +
        DefaultTerminalFontCandidates

    return candidates.firstNotNullOfOrNull { candidate ->
        availableFamilies[candidate.lowercase()]
    } ?: Font.MONOSPACED
}

private const val DefaultLineSpacing = 1.06f
private const val MinTerminalFontSize = 10
private const val MaxTerminalFontSize = 28
private const val MinScrollbackLines = 1000
private const val MaxScrollbackLines = 100000

private val DefaultTerminalFontCandidates = listOf(
    "JetBrains Mono",
    "SF Mono",
    "Menlo",
    "Consolas",
    Font.MONOSPACED,
)
