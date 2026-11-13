package app.thdev.moru.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FontPreferencesTest {
    @Test
    fun olderTerminalPreferencesReceiveIndependentIdeDefaults() {
        val settings = Json.decodeFromString<AppSettings>(
            """{"appearance":{"terminal_font_family":"Menlo","terminal_font_size":21}}""",
        )
        assertEquals(null, settings.appearance.uiFontFamily)
        assertEquals(14, settings.appearance.uiFontSize)
        assertEquals("Menlo", settings.appearance.terminalFontFamily)
        assertEquals(21, settings.appearance.terminalFontSize)
    }

    @Test
    fun bothFontPreferencesRoundTripIndependently() {
        val expected = AppSettings(appearance = AppearanceSettings(
            uiFontFamily = "Helvetica", uiFontSize = 16, terminalFontFamily = "SF Mono", terminalFontSize = 28,
        ))
        assertEquals(expected, Json.decodeFromString<AppSettings>(Json.encodeToString(expected)))
    }

    @Test
    fun removedInactiveUiKeysDoNotOverrideTheNewIdePreference() {
        val settings = Json { ignoreUnknownKeys = true }.decodeFromString<AppSettings>(
            """{"appearance":{"ui_font_family":"Old Font","ui_font_size":22}}""",
        )
        assertEquals(null, settings.appearance.uiFontFamily)
        assertEquals(14, settings.appearance.uiFontSize)
    }
}
