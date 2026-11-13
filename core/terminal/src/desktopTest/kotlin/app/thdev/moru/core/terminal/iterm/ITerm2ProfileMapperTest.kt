package app.thdev.moru.core.terminal.iterm

import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.core.model.terminal.TerminalImportItem
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ITerm2ProfileMapperTest {
    @Test
    fun theDefaultProfileKeepsSeparateLightAndDarkColorsAndReportsWhatMoruCannotDraw() {
        val profile = baseProfile() + colors(" (Light)", 1.0) + colors(" (Dark)", 0.0) + mapOf(
            "Guid" to "default",
            "Name" to "Work",
            "Use Separate Colors for Light and Dark Mode" to true,
            "Normal Font" to "D2Coding 13",
            "Vertical Spacing" to 1.2,
            "Cursor Type" to 1L,
            "Blinking Cursor" to true,
            "Scrollback Lines" to 1000L,
            "Option Key Sends" to 0L,
            "Transparency" to 0.1,
            "Keyboard Map" to mapOf("0xf700-0x220000" to mapOf("Action" to 10L)),
        )
        val other = baseProfile() + mapOf("Guid" to "other", "Name" to "Other")
        val result = mapITerm2Preferences(
            mapOf("New Bookmarks" to listOf(other, profile), "Default Bookmark Guid" to "default"),
            fontFamilyOf = { name -> if (name == "D2Coding") "D2Coding" else null },
        )

        val imported = assertIs<TerminalProfileImport.Imported>(result)
        assertEquals("iTerm2 · Work", imported.profile.source)
        assertEquals("#ffffff", imported.profile.light?.background)
        assertEquals("#000000", imported.profile.dark?.background)
        assertEquals(16, imported.profile.dark?.ansi?.size)
        assertEquals("D2Coding", imported.profile.fontFamily)
        assertEquals(13, imported.profile.fontSize)
        assertEquals(1000, imported.profile.scrollbackLines)
        assertEquals(1.2f, imported.profile.lineSpacing)
        assertEquals(TerminalCursorStyle.Bar, imported.profile.cursor)
        assertEquals(true, imported.profile.cursorBlink)
        assertEquals(
            listOf(
                TerminalImportItem.Colors, TerminalImportItem.LightDarkColors, TerminalImportItem.Font,
                TerminalImportItem.LineSpacing, TerminalImportItem.Cursor, TerminalImportItem.Scrollback,
            ),
            imported.applied,
        )
        assertEquals(listOf(TerminalImportItem.Transparency, TerminalImportItem.KeyMappings), imported.skipped)
    }

    @Test
    fun oneColorSetServesBothThemesAndAMissingFontIsSkipped() {
        val profile = baseProfile() + colors("", 0.5) + mapOf("Normal Font" to "NotInstalled-Regular 12.6", "Unlimited Scrollback" to true)
        val imported = assertIs<TerminalProfileImport.Imported>(
            mapITerm2Preferences(mapOf("New Bookmarks" to listOf(profile)), fontFamilyOf = { null }),
        )

        assertEquals(imported.profile.light, imported.profile.dark)
        assertEquals("#808080", imported.profile.light?.background)
        assertNull(imported.profile.fontFamily)
        assertNull(imported.profile.fontSize)
        assertEquals(50_000, imported.profile.scrollbackLines)
        assertEquals(listOf(TerminalImportItem.Font), imported.skipped)
    }

    @Test
    fun preferencesWithoutProfilesFail() {
        assertIs<TerminalProfileImport.Failed>(mapITerm2Preferences(emptyMap(), fontFamilyOf = { null }))
    }

    private fun baseProfile(): Map<String, Any?> = emptyMap()

    /** Foreground, background and ANSI colors, all of one gray [level], under keys ending in [suffix]. */
    private fun colors(suffix: String, level: Double): Map<String, Any?> {
        val color = mapOf("Red Component" to level, "Green Component" to level, "Blue Component" to level)
        return buildMap {
            put("Foreground Color$suffix", color)
            put("Background Color$suffix", color)
            (0..15).forEach { put("Ansi $it Color$suffix", color) }
        }
    }
}
