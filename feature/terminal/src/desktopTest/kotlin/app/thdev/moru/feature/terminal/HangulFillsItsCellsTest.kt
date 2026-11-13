package app.thdev.moru.feature.terminal

import app.thdev.moru.core.designsystem.theme.lightCkColors
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A Hangul syllable gets two cells of the Latin font. Drawn with the fallback font at its own size against the left
 * edge it filled three quarters of them, so "ab안녕cd" read as "ab안 녕 cd". Each syllable must now fill most of its
 * two cells, so Korean text reads as written.
 */
class HangulFillsItsCellsTest {
    @Test
    fun eachKoreanSyllableFillsMostOfItsTwoCells() {
        val settings = MoruTerminalSettingsProvider(null, 13, 1000, TerminalPalette.Holder(TerminalPalette.from(lightCkColors)))
        if (!settings.terminalFont.canDisplay('안')) return // No Hangul font on this machine.
        val widget = MoruJediTermWidget(40, 4, settings)
        val panel = widget.terminalPanel
        SwingUtilities.invokeAndWait {
            widget.setSize(600, 120)
            panel.setSize(600, 120)
            widget.terminal.writeCharacters("ab안녕cd")
        }
        Thread.sleep(300)
        val image = BufferedImage(600, 120, BufferedImage.TYPE_INT_RGB)
        SwingUtilities.invokeAndWait { panel.paint(image.createGraphics()) }

        val background = image.getRGB(599, 119)
        val inkedColumns = (0 until 600).filter { x -> (0 until 40).any { y -> image.getRGB(x, y) != background } }
        val glyphs = inkedColumns.fold(mutableListOf<IntRange>()) { runs, x ->
            val last = runs.lastOrNull()
            if (last != null && x == last.last + 1) runs[runs.lastIndex] = last.first..x else runs += x..x
            runs
        }
        // a, b, 안, 녕, c, d and the cursor.
        assertEquals(7, glyphs.size, "expected seven separate marks, found $glyphs")
        val cell = panel.getFontMetrics(settings.terminalFont).charWidth('W')
        val (b, an, nyeong, c) = listOf(glyphs[1], glyphs[2], glyphs[3], glyphs[4])
        // The two syllables together cover most of their four cells...
        val span = nyeong.last - an.first + 1
        assertTrue(span >= cell * 4 * 0.85, "안녕 covers $span px of its ${cell * 4} px ($glyphs)")
        // ...and no gap around them is wider than half a cell, so none of them reads as a typed space.
        val gaps = listOf(an.first - b.last - 1, nyeong.first - an.last - 1, c.first - nyeong.last - 1)
        assertTrue(gaps.all { gap -> gap <= cell / 2 }, "gaps around Korean are $gaps px with $cell px cells ($glyphs)")
    }
}
