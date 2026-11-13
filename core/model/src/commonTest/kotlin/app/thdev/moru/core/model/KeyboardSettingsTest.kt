package app.thdev.moru.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyboardSettingsTest {
    @Test
    fun everyShortcutStartsOnItsDefaultAndNoTwoDefaultsShareAChord() {
        val keyboard = KeyboardSettings()
        val chords = AppShortcut.entries.mapNotNull(keyboard::chordFor)

        assertEquals(ShortcutChord("1"), keyboard.chordFor(AppShortcut.OpenProject1))
        assertEquals(ShortcutChord("1", alt = true), keyboard.chordFor(AppShortcut.ShowWork))
        assertEquals(chords.size, chords.toSet().size)
        assertEquals(emptySet(), chords.toSet() intersect ShortcutChord.Reserved)
        assertEquals(AppShortcut.entries.size, AppShortcut.entries.map { it.id }.toSet().size)
    }

    @Test
    fun aChordMovesFromTheShortcutThatHeldItAndADefaultIsNotKeptAsAChange() {
        val moved = KeyboardSettings().assign(AppShortcut.ShowSettings, ShortcutChord("N"))

        assertEquals(ShortcutChord("N"), moved.chordFor(AppShortcut.ShowSettings))
        assertNull(moved.chordFor(AppShortcut.NewTask))

        val back = moved.assign(AppShortcut.ShowSettings, ShortcutChord(","))
        assertEquals(mapOf<String, ShortcutChord?>("new_task" to null), back.shortcuts)
    }

    @Test
    fun removingAShortcutKeepsTheRemovalSoTheDefaultDoesNotReturn() {
        val removed = KeyboardSettings().assign(AppShortcut.OpenProject9, null)

        assertNull(removed.chordFor(AppShortcut.OpenProject9))
        assertEquals(mapOf<String, ShortcutChord?>("open_project_9" to null), removed.shortcuts)
    }
}
