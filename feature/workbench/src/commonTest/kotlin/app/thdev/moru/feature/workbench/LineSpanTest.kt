package app.thdev.moru.feature.workbench

import kotlin.test.Test
import kotlin.test.assertEquals

class LineSpanTest {
    @Test
    fun emptyTextIsOneLine() {
        assertEquals(1, "".lineSpan())
    }

    @Test
    fun countsNewlineSeparatedLines() {
        assertEquals(3, "a\nb\nc".lineSpan())
    }

    @Test
    fun trailingNewlineAddsAnEmptyLine() {
        assertEquals(3, "a\nb\n".lineSpan())
    }
}
