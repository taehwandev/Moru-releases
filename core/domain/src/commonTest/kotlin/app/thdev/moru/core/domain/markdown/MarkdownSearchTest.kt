package app.thdev.moru.core.domain.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownSearchTest {
    @Test
    fun literalMatchingIgnoresCaseWithoutRegexOrOverlaps() {
        assertEquals(listOf(MarkdownTextRange(0, 3), MarkdownTextRange(4, 7)), MarkdownSearch.matches("ONE one", "one"))
        assertEquals(listOf(MarkdownTextRange(0, 2), MarkdownTextRange(2, 4)), MarkdownSearch.matches("aaaaa", "aa"))
        assertEquals(listOf(MarkdownTextRange(2, 5)), MarkdownSearch.matches("x [a] y", "[a]"))
        assertEquals(emptyList(), MarkdownSearch.matches("text", ""))
        assertEquals(emptyList(), MarkdownSearch.matches("text", "missing"))
    }

    @Test
    fun navigationWrapsFromSelectionAndStartsAtCaret() {
        val matches = MarkdownSearch.matches("a x a x a", "a")
        assertEquals(matches[1], MarkdownSearch.next(matches, matches[0]))
        assertEquals(matches.first(), MarkdownSearch.next(matches, matches.last()))
        assertEquals(matches.last(), MarkdownSearch.next(matches, matches.first(), backwards = true))
        assertEquals(matches[1], MarkdownSearch.next(matches, MarkdownTextRange(2)))
        assertEquals(matches[0], MarkdownSearch.next(matches, MarkdownTextRange(2), backwards = true))
        assertEquals(matches[1], MarkdownSearch.next(matches, MarkdownTextRange(1, 0)))
        assertEquals(null, MarkdownSearch.next(emptyList(), MarkdownTextRange.Zero))
    }

    @Test
    fun replaceCurrentRequiresAnExactMatchAndAdvances() {
        val value = MarkdownEditValue("One two ONE", MarkdownTextRange(0, 3))
        assertEquals(MarkdownEditValue("longer two ONE", MarkdownTextRange(11, 14)), MarkdownSearch.replaceCurrent(value, "one", "longer"))
        val reversed = value.copy(selection = MarkdownTextRange(4, 0))
        assertEquals("two ONE", MarkdownSearch.replaceCurrent(reversed, "one ", "").text)
        val cursor = value.copy(selection = MarkdownTextRange(1))
        assertEquals(cursor, MarkdownSearch.replaceCurrent(cursor, "one", "x"))
    }

    @Test
    fun replacementIsLiteralAndPreservesUnicodeCrLfAndTrailingLines() {
        val value = MarkdownEditValue("한글 😀\r\n한글\r\n")
        val result = MarkdownSearch.replaceAll(value, "한글", "\$1\\**")
        assertEquals("\$1\\** 😀\r\n\$1\\**\r\n", result.text)
        assertEquals(MarkdownTextRange(5), result.selection)
        assertEquals("aa", MarkdownSearch.replaceAll(MarkdownEditValue("a"), "a", "aa").text)
    }

    @Test
    fun noMatchesAndCompositionNeverMutateTextOrSelection() {
        val value = MarkdownEditValue("one", MarkdownTextRange(0, 3))
        for (query in listOf("", "missing")) {
            assertEquals(value, MarkdownSearch.replaceAll(value, query, "new"))
            assertEquals(value, MarkdownSearch.replaceCurrent(value, query, "new"))
        }
        val composing = value.copy(composition = MarkdownTextRange(0, 3))
        assertEquals(composing, MarkdownSearch.replaceAll(composing, "one", "new"))
        assertEquals(composing, MarkdownSearch.replaceCurrent(composing, "one", "new"))
    }

    @Test
    fun replacingAllIsOneUndoableSourceEdit() {
        val initial = MarkdownEditValue("one ONE", MarkdownTextRange(2))
        val history = MarkdownEditHistory(initial)
        history.accept(MarkdownSearch.replaceAll(initial, "one", "new"))
        assertEquals(initial, history.undo())
        assertEquals("new new", history.redo().text)
    }

}
