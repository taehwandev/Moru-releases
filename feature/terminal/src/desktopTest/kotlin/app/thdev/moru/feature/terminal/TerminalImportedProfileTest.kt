package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.core.model.terminal.TerminalProfile
import com.jediterm.core.Color as JediColor
import com.jediterm.terminal.CursorShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalImportedProfileTest {
    private val light = TerminalColorScheme("#111111", "#fafafa", selection = "#ccddee", ansi = List(16) { "#0000%02x".format(it) })
    private val dark = TerminalColorScheme("#eeeeee", "#101010", ansi = List(16) { "#%02x0000".format(it) })
    private val profile = TerminalProfile(
        source = "iTerm2 · Default", light = light, dark = dark,
        lineSpacing = 1.3f, cursor = TerminalCursorStyle.Underline, cursorBlink = false,
    )

    @Test
    fun thePaletteTakesTheImportedSetForTheCurrentTheme() {
        val lightPalette = TerminalPalette.from(lightCkColors, profile)
        assertEquals(JediColor(0xfa, 0xfa, 0xfa), lightPalette.canvas)
        assertEquals(JediColor(0xcc, 0xdd, 0xee), lightPalette.selection)
        assertEquals(JediColor(0, 0, 5), lightPalette.indexed(5))
        assertFalse(lightPalette.isDark)

        val darkPalette = TerminalPalette.from(darkCkColors, profile)
        assertEquals(JediColor(0x10, 0x10, 0x10), darkPalette.canvas)
        assertEquals(JediColor(9, 0, 0), darkPalette.indexed(9))
        assertEquals(TerminalPalette.from(darkCkColors).selection, darkPalette.selection)
        assertTrue(darkPalette.isDark)

        assertEquals(TerminalPalette.from(lightCkColors), TerminalPalette.from(lightCkColors, null))
    }

    @Test
    fun aSingleSetColorChangesOnlyItsSlotAndOnlyInItsTheme() {
        val moru = TerminalPalette.from(darkCkColors)
        val onlyRed = TerminalProfile(dark = TerminalColorScheme().with(TerminalColorSlot.Red, "#123456"))
        val palette = TerminalPalette.from(darkCkColors, onlyRed)
        assertEquals(JediColor(0x12, 0x34, 0x56), palette.indexed(1))
        assertEquals(moru.copy(ansi = moru.ansi.toMutableList().also { it[1] = JediColor(0x12, 0x34, 0x56) }), palette)
        assertEquals(TerminalPalette.from(lightCkColors), TerminalPalette.from(lightCkColors, onlyRed))
    }

    @Test
    fun theSettingsTakeSpacingAndCursorAndGiveThemBackWhenRemoved() {
        val settings = MoruTerminalSettingsProvider(
            null, 13, 1000, TerminalPalette.Holder(TerminalPalette.from(lightCkColors)),
        )
        assertEquals(1.06f, settings.lineSpacing)
        assertTrue(settings.altSendsEscape())
        assertEquals(CursorShape.BLINK_BLOCK, settings.cursorShape())

        assertTrue(settings.updateProfile(profile))
        assertEquals(1.3f, settings.lineSpacing)
        assertTrue(settings.altSendsEscape())
        assertEquals(CursorShape.STEADY_UNDERLINE, settings.cursorShape())
        assertFalse(settings.updateProfile(profile))

        assertTrue(settings.updateProfile(null))
        assertEquals(1.06f, settings.lineSpacing)
        assertTrue(settings.altSendsEscape())
    }

    @Test
    fun theOptionKeyFollowsItsOwnSettingNotTheProfile() {
        val settings = MoruTerminalSettingsProvider(
            null, 13, 1000, TerminalPalette.Holder(TerminalPalette.from(lightCkColors)), profile, optionSendsEscape = false,
        )
        assertFalse(settings.altSendsEscape())
        settings.optionSendsEscape = true
        assertTrue(settings.altSendsEscape())
    }
}
