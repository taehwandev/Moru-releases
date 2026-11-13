package app.thdev.moru.core.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals

class TextSearchLeafTest {
    @Test
    fun literalSearchIsCaseInsensitiveAndDoesNotTreatPunctuationAsRegex() {
        val leaf = TextSearchLeaf("one", "A.b a.B aaa")
        assertEquals(listOf(TextSearchMatch("one", 0, 3), TextSearchMatch("one", 4, 7)), leaf.matches("a.b"))
        assertEquals(emptyList(), leaf.matches(""))
        assertEquals(emptyList(), leaf.matches("longer than the whole text"))
        assertEquals(listOf(TextSearchMatch("one", 8, 10)), leaf.matches("aa"))
    }

    @Test
    fun unicodeOffsetsRetainOriginalSelectableCharacters() {
        val leaf = TextSearchLeaf("ko", "🐱 완료 완료")
        assertEquals(listOf(TextSearchMatch("ko", 3, 5), TextSearchMatch("ko", 6, 8)), leaf.matches("완료"))
    }
}
