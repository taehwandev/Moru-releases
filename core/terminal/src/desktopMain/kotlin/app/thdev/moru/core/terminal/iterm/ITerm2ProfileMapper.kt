package app.thdev.moru.core.terminal.iterm

import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.core.model.terminal.TerminalImportItem
import app.thdev.moru.core.model.terminal.TerminalProfile
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import kotlin.math.roundToInt

/**
 * Turns iTerm2's preferences into a Moru terminal profile. Only the default profile is read. [fontFamilyOf] maps the
 * PostScript name iTerm2 stores to an installed family, or null when the font is missing.
 */
internal fun mapITerm2Preferences(
    preferences: Map<String, Any?>,
    fontFamilyOf: (String) -> String?,
): TerminalProfileImport {
    val profiles = (preferences["New Bookmarks"] as? List<*>).orEmpty().filterIsInstance<Map<String, Any?>>()
    val defaultGuid = preferences["Default Bookmark Guid"] as? String
    val profile = profiles.firstOrNull { it["Guid"] == defaultGuid } ?: profiles.firstOrNull()
        ?: return TerminalProfileImport.Failed("iTerm2 has no profiles")
    val applied = mutableListOf<TerminalImportItem>()
    val skipped = mutableListOf<TerminalImportItem>()

    val separate = profile["Use Separate Colors for Light and Dark Mode"] == true
    val light = profile.colorScheme(if (separate) " (Light)" else "")
    val dark = profile.colorScheme(if (separate) " (Dark)" else "")
    if (light != null || dark != null) applied += TerminalImportItem.Colors
    if (separate && light != null && dark != null && light != dark) applied += TerminalImportItem.LightDarkColors

    val font = (profile["Normal Font"] as? String)?.let(::parseFont)
    val fontFamily = font?.let { fontFamilyOf(it.first) }
    val fontSize = font?.second?.takeIf { fontFamily != null }
    if (font != null) if (fontFamily != null) applied += TerminalImportItem.Font else skipped += TerminalImportItem.Font

    val lineSpacing = profile.number("Vertical Spacing")?.toFloat()?.takeIf { it in 0.5f..3f }
    if (lineSpacing != null) applied += TerminalImportItem.LineSpacing

    val cursor = when (profile.number("Cursor Type")?.toInt()) {
        0 -> TerminalCursorStyle.Underline
        1 -> TerminalCursorStyle.Bar
        2 -> TerminalCursorStyle.Block
        else -> null
    }
    val blink = profile["Blinking Cursor"] as? Boolean
    if (cursor != null || blink != null) applied += TerminalImportItem.Cursor

    val scrollback = when {
        profile["Unlimited Scrollback"] == true -> MaxScrollback
        else -> profile.number("Scrollback Lines")?.toInt()?.coerceIn(MinScrollback, MaxScrollback)
    }
    if (scrollback != null) applied += TerminalImportItem.Scrollback

    if ((profile.number("Transparency") ?: 0.0) > 0.0) skipped += TerminalImportItem.Transparency
    if (profile["Blur"] == true) skipped += TerminalImportItem.Blur
    if (!(profile["Background Image Location"] as? String).isNullOrBlank()) skipped += TerminalImportItem.BackgroundImage
    if ((profile.number("Horizontal Spacing") ?: 1.0) != 1.0) skipped += TerminalImportItem.ColumnSpacing
    if (profile.color("Cursor Color") != null) skipped += TerminalImportItem.CursorColor
    if (profile["Use Non-ASCII Font"] == true) skipped += TerminalImportItem.NonAsciiFont
    if (profile["ASCII Ligatures"] == true || profile["Non-ASCII Ligatures"] == true) skipped += TerminalImportItem.Ligatures
    if ((profile["Keyboard Map"] as? Map<*, *>).orEmpty().isNotEmpty()) skipped += TerminalImportItem.KeyMappings
    if ((profile["Triggers"] as? List<*>).orEmpty().isNotEmpty()) skipped += TerminalImportItem.Triggers

    val name = (profile["Name"] as? String)?.takeIf(String::isNotBlank) ?: "Default"
    return TerminalProfileImport.Imported(
        profile = TerminalProfile(
            source = "iTerm2 · $name",
            light = light,
            dark = dark,
            lineSpacing = lineSpacing,
            cursor = cursor,
            cursorBlink = blink,
            fontFamily = fontFamily,
            fontSize = fontSize,
            scrollbackLines = scrollback,
        ),
        applied = applied,
        skipped = skipped,
    )
}

/** Reads `Foreground Color`, `Ansi 0 Color` and the rest with [suffix] (` (Light)`, ` (Dark)` or none). */
private fun Map<String, Any?>.colorScheme(suffix: String): TerminalColorScheme? {
    fun named(key: String) = color("$key$suffix") ?: color(key)
    val foreground = named("Foreground Color") ?: return null
    val background = named("Background Color") ?: return null
    val ansi = (0..15).map { index -> named("Ansi $index Color") }
    return TerminalColorScheme(
        foreground = foreground,
        background = background,
        selection = named("Selection Color"),
        link = named("Link Color"),
        ansi = if (ansi.all { it != null }) ansi.filterNotNull() else emptyList(),
    )
}

private fun Map<String, Any?>.color(key: String): String? {
    val components = this[key] as? Map<*, *> ?: return null
    fun channel(name: String): Int? =
        (components["$name Component"] as? Number)?.toDouble()?.let { (it.coerceIn(0.0, 1.0) * 255).roundToInt() }
    val red = channel("Red") ?: return null
    val green = channel("Green") ?: return null
    val blue = channel("Blue") ?: return null
    return "#%02x%02x%02x".format(red, green, blue)
}

private fun Map<String, Any?>.number(key: String): Double? = (this[key] as? Number)?.toDouble()

/** `MesloLGS-NF-Regular 13` → PostScript name and whole point size. */
private fun parseFont(value: String): Pair<String, Int>? {
    val name = value.substringBeforeLast(' ', "").takeIf(String::isNotBlank) ?: return null
    val size = value.substringAfterLast(' ').toDoubleOrNull()?.roundToInt() ?: return null
    return name to size.coerceIn(MinFontSize, MaxFontSize)
}

private const val MinScrollback = 500
private const val MaxScrollback = 50_000
private const val MinFontSize = 10
private const val MaxFontSize = 28
