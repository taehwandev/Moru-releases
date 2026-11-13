package app.thdev.moru.core.model.terminal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the terminal looks and how much it keeps, set in Settings or imported from another terminal app. A null field
 * keeps Moru's own choice, so a profile only changes what the user or the source actually set.
 */
@Serializable
data class TerminalProfile(
    /** Where it came from, such as `iTerm2 · Default`; null when it was made in Moru. */
    val source: String? = null,
    /** Colors while Moru uses a light theme, and while it uses a dark one; null follows Moru. */
    val light: TerminalColorScheme? = null,
    val dark: TerminalColorScheme? = null,
    /** Line height as a multiple of the font's own; 1.0 is the font's natural spacing. */
    @SerialName("line_spacing")
    val lineSpacing: Float? = null,
    val cursor: TerminalCursorStyle? = null,
    @SerialName("cursor_blink")
    val cursorBlink: Boolean? = null,
    /** Font and scrollback the source used; they stand in for the user's own only while this profile is kept. */
    @SerialName("font_family")
    val fontFamily: String? = null,
    @SerialName("font_size")
    val fontSize: Int? = null,
    @SerialName("scrollback_lines")
    val scrollbackLines: Int? = null,
) {
    /** The colors for a light or dark Moru theme, or null to keep Moru's. */
    fun colorsFor(light: Boolean): TerminalColorScheme? = if (light) this.light else dark

    /** This profile with the colors for a light or dark Moru theme replaced; an empty scheme becomes Moru's. */
    fun withColors(light: Boolean, scheme: TerminalColorScheme?): TerminalProfile {
        val kept = scheme?.takeUnless { it.isEmpty }
        return if (light) copy(light = kept) else copy(dark = kept)
    }
}
