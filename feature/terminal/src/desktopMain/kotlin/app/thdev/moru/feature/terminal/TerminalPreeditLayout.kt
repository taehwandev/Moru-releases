package app.thdev.moru.feature.terminal

import com.jediterm.terminal.util.CharUtils
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.text.BreakIterator

/** One cell-aware layout shared by native candidate placement and marked-text painting. */
internal class TerminalPreeditLayout(text: String, origin: Point, columns: Int) {
    private data class Glyph(val text: String, val start: Int, val end: Int, val cell: Point, val width: Int)
    private val glyphs = mutableListOf<Glyph>()
    val end: Point

    init {
        val limit = columns.coerceAtLeast(1)
        var position = Point(origin)
        val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
        var start = iterator.first()
        var finish = iterator.next()
        while (finish != BreakIterator.DONE) {
            val cluster = text.substring(start, finish)
            val width = cluster.codePoints().toArray().maxOfOrNull { cellWidth(it) }?.coerceAtMost(limit) ?: 0
            if (position.x + width > limit) position = Point(0, position.y + 1)
            glyphs += Glyph(cluster, start, finish, Point(position), width)
            position.x += width
            start = finish
            finish = iterator.next()
        }
        end = position
    }

    fun location(offset: Int, cellSize: Dimension, inset: Int): Rectangle {
        val glyph = glyphs.firstOrNull { offset < it.end }
        val position = glyph?.cell ?: end
        return Rectangle(inset + position.x * cellSize.width, (position.y + 1) * cellSize.height, 0, 0)
    }

    fun paint(
        graphics: Graphics2D,
        cellSize: Dimension,
        inset: Int,
        foreground: Color,
        background: Color,
        font: (CharArray) -> Font,
    ) {
        for (glyph in glyphs) {
            val x = inset + glyph.cell.x * cellSize.width
            val y = glyph.cell.y * cellSize.height
            val width = glyph.width * cellSize.width
            val chars = glyph.text.toCharArray()
            graphics.color = background
            graphics.fillRect(x, y, width, cellSize.height)
            graphics.color = foreground
            graphics.font = font(chars)
            val metrics = graphics.fontMetrics
            val padding = ((width - metrics.charsWidth(chars, 0, chars.size)) / 2).coerceAtLeast(0)
            val baseline = y + cellSize.height - metrics.descent
            graphics.drawChars(chars, 0, chars.size, x + padding, baseline)
            graphics.drawLine(x, y + cellSize.height - 2, x + width - 1, y + cellSize.height - 2)
        }
    }

    private fun cellWidth(codePoint: Int): Int = when {
        Character.getType(codePoint) in CombiningTypes || codePoint == 0x200d -> 0
        CharUtils.isDoubleWidthCharacter(codePoint, false) -> 2
        else -> 1
    }

    private companion object {
        val CombiningTypes = setOf(
            Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt(),
        )
    }
}
