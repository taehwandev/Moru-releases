package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.lightCkColors
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalFontPreferencesTest {
    @Test
    fun rendererAcceptsTheEntireSettingsFontSizeRange() {
        for (size in listOf(10, 13, 24, 28)) {
            val settings = MoruTerminalSettingsProvider(
                fontFamily = null, fontSize = size, scrollbackLines = 5000,
                paletteHolder = TerminalPalette.Holder(TerminalPalette.from(lightCkColors)),
            )
            assertEquals(size.toFloat(), settings.terminalFontSize)
            assertEquals(size, settings.terminalFont.size)
        }
    }
}
