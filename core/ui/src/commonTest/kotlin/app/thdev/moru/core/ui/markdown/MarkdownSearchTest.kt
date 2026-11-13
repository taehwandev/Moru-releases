package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownSearchTest {
    @Test
    fun literalMatchingIgnoresCaseWithoutRegexOrOverlaps() {
        assertEquals(listOf(TextRange(0, 3), TextRange(4, 7)), MarkdownSearch.matches("ONE one", "one"))
        assertEquals(listOf(TextRange(0, 2), TextRange(2, 4)), MarkdownSearch.matches("aaaaa", "aa"))
        assertEquals(listOf(TextRange(2, 5)), MarkdownSearch.matches("x [a] y", "[a]"))
        assertEquals(emptyList(), MarkdownSearch.matches("text", ""))
        assertEquals(emptyList(), MarkdownSearch.matches("text", "missing"))
    }

    @Test
    fun navigationWrapsFromSelectionAndStartsAtCaret() {
        val matches = MarkdownSearch.matches("a x a x a", "a")
        assertEquals(matches[1], MarkdownSearch.next(matches, matches[0]))
        assertEquals(matches.first(), MarkdownSearch.next(matches, matches.last()))
        assertEquals(matches.last(), MarkdownSearch.next(matches, matches.first(), backwards = true))
        assertEquals(matches[1], MarkdownSearch.next(matches, TextRange(2)))
        assertEquals(matches[0], MarkdownSearch.next(matches, TextRange(2), backwards = true))
        assertEquals(matches[1], MarkdownSearch.next(matches, TextRange(1, 0)))
        assertEquals(null, MarkdownSearch.next(emptyList(), TextRange.Zero))
    }

    @Test
    fun replaceCurrentRequiresAnExactMatchAndAdvances() {
        val value = TextFieldValue("One two ONE", TextRange(0, 3))
        assertEquals(TextFieldValue("longer two ONE", TextRange(11, 14)), MarkdownSearch.replaceCurrent(value, "one", "longer"))
        val reversed = value.copy(selection = TextRange(4, 0))
        assertEquals("two ONE", MarkdownSearch.replaceCurrent(reversed, "one ", "").text)
        val cursor = value.copy(selection = TextRange(1))
        assertEquals(cursor, MarkdownSearch.replaceCurrent(cursor, "one", "x"))
    }

    @Test
    fun replacementIsLiteralAndPreservesUnicodeCrLfAndTrailingLines() {
        val value = TextFieldValue("한글 😀\r\n한글\r\n")
        val result = MarkdownSearch.replaceAll(value, "한글", "\$1\\**")
        assertEquals("\$1\\** 😀\r\n\$1\\**\r\n", result.text)
        assertEquals(TextRange(5), result.selection)
        assertEquals("aa", MarkdownSearch.replaceAll(TextFieldValue("a"), "a", "aa").text)
    }

    @Test
    fun noMatchesAndCompositionNeverMutateTextOrSelection() {
        val value = TextFieldValue("one", TextRange(0, 3))
        for (query in listOf("", "missing")) {
            assertEquals(value, MarkdownSearch.replaceAll(value, query, "new"))
            assertEquals(value, MarkdownSearch.replaceCurrent(value, query, "new"))
        }
        val composing = value.copy(composition = TextRange(0, 3))
        assertEquals(composing, MarkdownSearch.replaceAll(composing, "one", "new"))
        assertEquals(composing, MarkdownSearch.replaceCurrent(composing, "one", "new"))
    }

    @Test
    fun replacingAllIsOneUndoableSourceEdit() {
        val initial = TextFieldValue("one ONE", TextRange(2))
        val history = MarkdownEditHistory(initial)
        history.accept(MarkdownSearch.replaceAll(initial, "one", "new"))
        assertEquals(initial, history.undo())
        assertEquals("new new", history.redo().text)
    }

    @Test
    fun activeHighlightPreservesSourceOffsetsInLiveAndSourceModes() {
        val source = AnnotatedString("**one** two")
        for (base in listOf(VisualTransformation.None, MarkdownLiveTransformation(TextRange(2, 5)))) {
            val result = MarkdownSearchHighlight(base, TextRange(2, 5), Color.Blue).filter(source)
            val start = result.offsetMapping.originalToTransformed(2)
            val end = result.offsetMapping.originalToTransformed(5)
            assertEquals("one", result.text.text.substring(start, end))
            assertEquals(Color.Blue.copy(alpha = 0.25f), result.text.spanStyles.last().item.background)
        }
    }
}
