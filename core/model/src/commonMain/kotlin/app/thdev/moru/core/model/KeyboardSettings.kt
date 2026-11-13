package app.thdev.moru.core.model

import kotlinx.serialization.Serializable

/**
 * The app shortcuts the user changed, by [AppShortcut.id]; a null value removes that shortcut. Every shortcut not
 * listed keeps its default, so a new default reaches users who never touched it.
 */
@Serializable
data class KeyboardSettings(
    val shortcuts: Map<String, ShortcutChord?> = emptyMap(),
) {
    /** The chord that triggers [shortcut] now, or null when it has none. */
    fun chordFor(shortcut: AppShortcut): ShortcutChord? =
        if (shortcut.id in shortcuts) shortcuts[shortcut.id] else shortcut.default

    /**
     * Gives [shortcut] the [chord] (null removes it). A chord belongs to one shortcut at a time, so any other shortcut
     * holding it loses it. A shortcut set back to its default is no longer kept as a change.
     */
    fun assign(shortcut: AppShortcut, chord: ShortcutChord?): KeyboardSettings {
        val changed = shortcuts.toMutableMap()
        if (chord != null) {
            AppShortcut.entries.filter { other -> other != shortcut && chordFor(other) == chord }
                .forEach { other -> changed[other.id] = null }
        }
        if (chord == shortcut.default) changed.remove(shortcut.id) else changed[shortcut.id] = chord
        return KeyboardSettings(changed)
    }
}
