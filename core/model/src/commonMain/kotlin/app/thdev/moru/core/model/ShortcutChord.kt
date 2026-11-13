package app.thdev.moru.core.model

import kotlinx.serialization.Serializable

/**
 * A key pressed with the platform modifier (Command on macOS, Control elsewhere), plus Shift or Option when set.
 * [key] names the key as printed on a US layout, such as "K", "1", "[" or "F5".
 */
@Serializable
data class ShortcutChord(
    val key: String,
    val shift: Boolean = false,
    val alt: Boolean = false,
) {
    companion object {
        /**
         * Chords the app never takes because the terminal and text fields need them (copy, paste, cut, select all,
         * undo, redo) or the system does (hide, minimise, plain quit).
         */
        val Reserved: Set<ShortcutChord> =
            listOf("C", "V", "X", "A", "Z", "H", "M", "Q").map { ShortcutChord(it) }.toSet() + ShortcutChord("Z", shift = true)
    }
}
