package app.thdev.moru.feature.terminal

import com.jediterm.core.Color as JediColor
import com.jediterm.terminal.ArrayTerminalDataStream
import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.emulator.JediEmulator
import com.jediterm.terminal.ui.JediTermWidget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MoruJediTermAnsiColorTest {
    @Test
    fun ansi16Color256AndTruecolorOutputResolveForForegroundAndBackground() {
        val palette = testTerminalPalette()
        val settingsProvider = MoruTerminalSettingsProvider(
            fontFamily = null,
            fontSize = 14,
            scrollbackLines = 1_000,
            paletteHolder = TerminalPalette.Holder(palette),
        )
        val widget = JediTermWidget(80, 24, settingsProvider)

        try {
            assertNotEquals(settingsProvider.defaultForeground, settingsProvider.defaultBackground)
            assertEquals(palette.foreground, settingsProvider.defaultForeground.toColor())
            assertEquals(palette.canvas, settingsProvider.defaultBackground.toColor())

            val output = "\u001B[31;41mA\u001B[38;5;196;48;5;22mB\u001B[38;2;17;34;51;48;2;68;85;102mC\u001B[0m"
            val emulator = JediEmulator(
                ArrayTerminalDataStream(output.toCharArray()),
                widget.terminal,
            )

            while (emulator.hasNext()) {
                emulator.next()
            }

            val colorPalette = settingsProvider.terminalColorPalette
            assertStyleColors(widget, 0, colorPalette, palette.ansi[1], palette.ansi[1])
            assertStyleColors(widget, 1, colorPalette, JediColor(255, 0, 0), JediColor(0, 95, 0))
            assertStyleColors(widget, 2, colorPalette, JediColor(17, 34, 51), JediColor(68, 85, 102))

            assertEquals(palette.foreground, colorPalette.getForeground(TerminalColor.index(-2)))
            assertEquals(palette.canvas, colorPalette.getBackground(TerminalColor.index(-2)))
            assertEquals(palette.foreground, colorPalette.getForeground(TerminalColor.index(256)))
            assertEquals(palette.canvas, colorPalette.getBackground(TerminalColor.index(256)))
        } finally {
            widget.close()
        }
    }

    private fun assertStyleColors(
        widget: JediTermWidget,
        column: Int,
        colorPalette: com.jediterm.terminal.emulator.ColorPalette,
        expectedForeground: JediColor,
        expectedBackground: JediColor,
    ) {
        val style = requireNotNull(widget.terminalTextBuffer.getStyleAt(column, 0))
        assertEquals(expectedForeground, colorPalette.getForeground(requireNotNull(style.foreground)))
        assertEquals(expectedBackground, colorPalette.getBackground(requireNotNull(style.background)))
    }

    private fun testTerminalPalette(): TerminalPalette {
        val ansi = List(16) { index -> JediColor(index, index + 1, index + 2) }
        return TerminalPalette(
            canvas = JediColor(1, 2, 3),
            foreground = JediColor(4, 5, 6),
            selection = JediColor(7, 8, 9),
            find = JediColor(10, 11, 12),
            link = JediColor(13, 14, 15),
            ansi = ansi,
        )
    }
}
