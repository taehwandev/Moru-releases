package app.thdev.moru.core.model.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalColorSchemeTest {
    private val moru = TerminalColorScheme("#eeeeee", "#111111", "#333333", "#4488ff", List(16) { "#0000${(10 + it).toString(16)}" })

    @Test
    fun oneSetColorOverridesItsSlotAndTheRestFollowMoru() {
        val scheme = TerminalColorScheme().with(TerminalColorSlot.Red, "#ff0000")
        val shown = scheme.over(moru)
        assertEquals("#ff0000", shown[TerminalColorSlot.Red])
        assertEquals(moru.background, shown.background)
        assertEquals(moru.ansi[2], shown[TerminalColorSlot.Green])
        assertNull(scheme[TerminalColorSlot.Green])
    }

    @Test
    fun clearingTheLastColorLeavesNothingAndTheProfileFallsBackToMoru() {
        val scheme = TerminalColorScheme().with(TerminalColorSlot.Background, "#101010").with(TerminalColorSlot.Background, null)
        assertTrue(scheme.isEmpty)
        assertNull(TerminalProfile().withColors(light = false, scheme).dark)
    }

    @Test
    fun theLightAndDarkThemesKeepTheirOwnColors() {
        val profile = TerminalProfile(dark = moru)
        assertEquals(moru, profile.colorsFor(light = false))
        assertNull(profile.colorsFor(light = true))
    }

    @Test
    fun everyPresetSetsAllSixteenColors() {
        TerminalColorPresets.all.forEach { preset ->
            assertEquals(16, preset.scheme.ansi.size, preset.name)
            assertTrue(preset.scheme.ansi.all { it != null && it.matches(Regex("#[0-9a-f]{6}")) }, preset.name)
            assertEquals(preset, TerminalColorPresets.matching(preset.scheme))
        }
    }
}
