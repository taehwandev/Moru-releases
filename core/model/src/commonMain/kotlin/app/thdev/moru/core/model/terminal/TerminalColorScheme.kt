package app.thdev.moru.core.model.terminal

import kotlinx.serialization.Serializable

/**
 * A terminal's colors as `#rrggbb`. Each color is an override: null keeps Moru's own for that slot, so changing one
 * color leaves the rest following the Moru theme. [ansi] is empty or holds the 16 ANSI colors in order.
 */
@Serializable
data class TerminalColorScheme(
    val foreground: String? = null,
    val background: String? = null,
    val selection: String? = null,
    val link: String? = null,
    val ansi: List<String?> = emptyList(),
) {
    /** The color in [slot], or null when it follows Moru. */
    operator fun get(slot: TerminalColorSlot): String? = when (slot) {
        TerminalColorSlot.Foreground -> foreground
        TerminalColorSlot.Background -> background
        TerminalColorSlot.Selection -> selection
        else -> ansi.getOrNull(slot.ansiIndex)
    }

    /** This scheme with [slot] set to [hex], or back to Moru's when [hex] is null. */
    fun with(slot: TerminalColorSlot, hex: String?): TerminalColorScheme = when (slot) {
        TerminalColorSlot.Foreground -> copy(foreground = hex)
        TerminalColorSlot.Background -> copy(background = hex)
        TerminalColorSlot.Selection -> copy(selection = hex)
        else -> copy(ansi = List(16) { index -> if (index == slot.ansiIndex) hex else ansi.getOrNull(index) })
    }

    /** Every slot this scheme sets, filled from [base] where it follows Moru. */
    fun over(base: TerminalColorScheme): TerminalColorScheme = TerminalColorScheme(
        foreground = foreground ?: base.foreground,
        background = background ?: base.background,
        selection = selection ?: base.selection,
        link = link ?: base.link,
        ansi = List(16) { index -> ansi.getOrNull(index) ?: base.ansi.getOrNull(index) },
    )

    /** True when nothing is overridden. */
    val isEmpty: Boolean
        get() = foreground == null && background == null && selection == null && link == null && ansi.all { it == null }
}
