package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownLiveTransformationTest {
    private fun render(source: String, selection: TextRange = TextRange(source.length), composition: TextRange? = null) =
        MarkdownLiveTransformation(selection, composition).filter(AnnotatedString(source))

    @Test
    fun caretRevealsOnlyItsLineWithoutChangingSource() {
        val source = "# Title\n**bold** and *italic*\n[Guide](guide.md)\n"
        val result = render(source, TextRange(12))
        assertEquals("Title\n**bold** and *italic*\nGuide\n", result.text.text)
        assertEquals("# Title\n**bold** and *italic*\n[Guide](guide.md)\n", source)
        assertEquals(source.indexOf("Guide"), result.offsetMapping.transformedToOriginal(result.text.text.indexOf("Guide")))
    }

    @Test
    fun selectedAndComposingLinesRemainLiteral() {
        val source = "**one**\n*two*\n~~three~~\n"
        assertEquals("**one**\n*two*\nthree\n", render(source, TextRange(1, 12)).text.text)
        assertEquals("**one**\n*two*\nthree\n", render(source, TextRange(1), TextRange(9, 11)).text.text)
    }

    @Test
    fun sourceOffsetsStayMonotoneAndWithinBoundsForEveryCaret() {
        val source = "# 한글 😀\r\n**bold** [link](path.md) `code`\r\n\n"
        for (caret in 0..source.length) {
            val result = render(source, TextRange(caret))
            val forward = (0..source.length).map(result.offsetMapping::originalToTransformed)
            val backward = (0..result.text.length).map(result.offsetMapping::transformedToOriginal)
            assertTrue(forward.zipWithNext().all { (a, b) -> a <= b })
            assertTrue(backward.zipWithNext().all { (a, b) -> a <= b })
            assertTrue(forward.all { it in 0..result.text.length })
            assertTrue(backward.all { it in 0..source.length })
            for (i in result.text.indices) {
                val original = backward[i]
                assertEquals(result.text[i], source[original])
            }
        }
    }

    @Test
    fun fencedAndIndentedCodeKeepProseMarkersLiteral() {
        val source = "````kotlin\n# **literal**\n```\n[not a link](x)\n````\n    **indented**\n"
        assertEquals("\n# **literal**\n```\n[not a link](x)\n\n    **indented**\n", render(source).text.text)
    }

    @Test
    fun codeSpansEscapesImagesAndIncompleteSyntaxAreConservative() {
        val source = "`**code**` \\*literal\\* ![image](x.png) [unfinished](x\n"
        assertEquals("**code** *literal* ![image](x.png) [unfinished](x\n", render(source).text.text)
        assertEquals("**escaped**\n", render("**escaped\\**\n").text.text)
        assertEquals("``unmatched`\n", render("``unmatched`\n").text.text)
    }

    @Test
    fun formattingProducesSpansAndDoesNotEatWordUnderscores() {
        val result = render("**bold** *italic* ~~strike~~ __strong__ snake_case_name\n")
        assertEquals("bold italic strike strong snake_case_name\n", result.text.text)
        assertTrue(result.text.spanStyles.size >= 4)
    }

    @Test
    fun emptyAndTrailingNewlinesHaveValidEndMappings() {
        for (source in listOf("", "\n", "# Heading\n\n", "~~~\n")) {
            val result = render(source)
            assertEquals(result.text.length, result.offsetMapping.originalToTransformed(source.length))
            assertEquals(source.length, result.offsetMapping.transformedToOriginal(result.text.length))
        }
    }

    @Test
    fun aKeptCacheGivesWhatAFreshReadingWouldThroughEditsAndCaretMoves() {
        val cache = MarkdownLiveCache()
        val steps = listOf(
            "# Title\n**bold** and *it" to 5,
            "# Title\n**bold** and *italic*\n" to 3,
            "# Title\n**bold** and *italic*\n" to 20,
            "# Title\n**bold** and *italic*\n```\ncode\n```\n[Guide](g.md)" to 2,
            "# Title\n**bold** and *italic*\n```\ncode\n```\n[Guide](g.md)" to 50,
        )
        steps.forEach { (source, caret) ->
            val kept = MarkdownLiveTransformation(TextRange(caret), cache = cache).filter(AnnotatedString(source))
            val fresh = render(source, TextRange(caret))
            assertEquals(fresh.text, kept.text, "caret $caret in $source")
            for (offset in 0..source.length) {
                assertEquals(fresh.offsetMapping.originalToTransformed(offset), kept.offsetMapping.originalToTransformed(offset))
            }
        }
    }

    @Test
    fun aCaretMoveWithinTheSameLineReusesTheWholeResult() {
        val cache = MarkdownLiveCache()
        val source = "**one**\n*two*\n"
        val first = MarkdownLiveTransformation(TextRange(1), cache = cache).filter(AnnotatedString(source))
        val second = MarkdownLiveTransformation(TextRange(4), cache = cache).filter(AnnotatedString(source))
        assertTrue(first === second)
    }
}
