package app.thdev.moru.feature.terminal

import java.awt.Font
import java.awt.geom.AffineTransform

/**
 * The font to draw a two-cell character (Hangul, CJK) with, so it fills its two cells and sits in their middle.
 * The terminal gives such a character two cells of the Latin font, but the fallback font's glyph is narrower and
 * was drawn against the left edge, so "안녕" read as "안 녕". The glyph grows a little (never past [MaxGrowth] in
 * either direction, so it keeps its shape and stays inside the line) and is shifted to the middle of the two cells.
 */
internal fun wideGlyphFont(base: Font, glyphAdvance: Float, cellWidth: Int): Font {
    if (glyphAdvance <= 0f || cellWidth <= 0) return base
    val slot = cellWidth * 2f
    val wanted = slot * FillRatio / glyphAdvance
    val size = wanted.coerceIn(1f, MaxGrowth)
    val stretch = (wanted / size).coerceIn(1f, MaxGrowth)
    val drawnAdvance = glyphAdvance * size * stretch
    val shift = ((slot - drawnAdvance) / 2f).coerceAtLeast(0f)
    return base
        .deriveFont(base.size2D * size)
        .deriveFont(AffineTransform(stretch.toDouble(), 0.0, 0.0, 1.0, shift.toDouble(), 0.0))
}

private const val FillRatio = 0.9f
private const val MaxGrowth = 1.12f
