package app.thdev.moru.feature.terminal

import java.awt.GraphicsEnvironment
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoruPreeditTest {
    @Test
    fun aCaretOnlyInputMethodEventKeepsEnterWithTheActiveKoreanComposition() {
        PreeditTestFixture().use { fixture ->
            fixture.composition("하")
            fixture.caretChanged(1)
            fixture.keyPressed(KeyEvent.VK_ENTER)

            assertFalse(
                fixture.connector.hasWriteWithin(150),
                "A text-null caret update must preserve composition and keep Enter out of the PTY.",
            )
            fixture.composition("하", committedCharacters = 1)
            fixture.keyPressed(KeyEvent.VK_ENTER)
            assertTrue(fixture.connector.awaitText("하\r"))
            assertEquals("하\r", fixture.connector.writtenText(), "Commit and the subsequent Enter must each be sent once.")
        }
    }

    @Test
    fun theCandidateAndPaintedPreeditStayTogetherWhileAHiddenCursorTuiRedraws() {
        if (GraphicsEnvironment.isHeadless()) return // Native screen coordinates require a display.
        PreeditTestFixture(showWindow = true).use { fixture ->
            val empty = fixture.paint()
            fixture.composition("한")
            val candidate = fixture.candidateLocation()
            val composing = fixture.paint()
            val underline = longestChangedRow(empty, composing)
            assertEquals(candidate.x, underline.columns.first, "Preedit must start at its candidate location, without a one-cell offset.")

            // A TUI paints a status area elsewhere while keeping its native cursor hidden.
            fixture.output("\u001B[1;35Hstatus\u001B[8;50H")
            assertEquals(candidate, fixture.candidateLocation(), "Unrelated TUI cursor moves must not move the active IME anchor.")
            val afterRedraw = fixture.paint()
            for (y in 0 until composing.height) {
                for (x in 0 until composing.width) {
                    if (empty.getRGB(x, y) != composing.getRGB(x, y)) {
                        assertEquals(
                            composing.getRGB(x, y),
                            afterRedraw.getRGB(x, y),
                            "TUI redraw must preserve preedit pixels at ($x, $y).",
                        )
                    }
                }
            }
            fixture.composition("")
            val cleared = fixture.paint()
            val overlayColumns = changedColumns(cleared, afterRedraw)
            assertEquals(underline.columns, overlayColumns, "TUI redraw must not leave a second preedit at its moved cursor.")
        }
    }

    @Test
    fun KoreanPreeditUsesTwoCellsAndTheSameGlyphWidthAsCommittedTextThenCancelsCleanly() {
        PreeditTestFixture().use { fixture ->
            val empty = fixture.paint()
            fixture.composition("한")
            val composing = fixture.paint()
            val underline = longestChangedRow(empty, composing)
            assertTrue(
                underline.columns.count() in (fixture.cellWidth * 2)..(fixture.cellWidth * 2 + 1),
                "The Hangul composition underline must cover two cells; got ${underline.columns} with ${fixture.cellWidth}px cells.",
            )
            val preeditGlyphWidth = changedColumns(empty, composing, beforeRow = underline.y).count()

            fixture.composition("")
            assertImagesEqual(empty, fixture.paint(), "Cancelling marked text must remove the entire overlay.")
            fixture.keyPressed(KeyEvent.VK_ENTER)
            assertTrue(fixture.connector.awaitText("\r"), "Normal Enter handling must resume after cancellation.")

            fixture.output("\u001B[3;8H한")
            val committedGlyphWidth = changedColumns(empty, fixture.paint()).count()
            assertTrue(preeditGlyphWidth > 0, "The composing Hangul glyph must be visible.")
            assertEquals(committedGlyphWidth, preeditGlyphWidth, "Preedit must use the terminal's fitted Hangul font.")
        }
    }

    private data class ChangedRow(val y: Int, val columns: IntRange)

    /** The underline is the longest solid painted run; ties prefer the lowest row beneath the glyph. */
    private fun longestChangedRow(before: BufferedImage, after: BufferedImage): ChangedRow {
        var longest = ChangedRow(0, IntRange.EMPTY)
        for (y in 0 until after.height) {
            var start = -1
            for (x in 0..after.width) {
                val changed = x < after.width && before.getRGB(x, y) != after.getRGB(x, y)
                if (changed && start < 0) start = x
                if (!changed && start >= 0) {
                    val columns = start until x
                    if (columns.count() >= longest.columns.count()) longest = ChangedRow(y, columns)
                    start = -1
                }
            }
        }
        assertTrue(!longest.columns.isEmpty(), "Composition must produce a visible glyph and underline.")
        return longest
    }

    private fun changedColumns(before: BufferedImage, after: BufferedImage, beforeRow: Int = after.height): IntRange {
        val columns = (0 until after.width).filter { x ->
            (0 until beforeRow.coerceAtLeast(0)).any { y -> before.getRGB(x, y) != after.getRGB(x, y) }
        }
        return if (columns.isEmpty()) IntRange.EMPTY else columns.first()..columns.last()
    }

    private fun assertImagesEqual(before: BufferedImage, after: BufferedImage, message: String) {
        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        for (y in 0 until before.height) {
            for (x in 0 until before.width) assertEquals(before.getRGB(x, y), after.getRGB(x, y), "$message ($x, $y)")
        }
    }
}
