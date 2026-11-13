package app.thdev.moru.core.domain.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownLivePresentationTest {
    private fun render(source: String, selection: MarkdownTextRange = MarkdownTextRange(source.length), composition: MarkdownTextRange? = null) =
        MarkdownLivePresentation(selection, composition).filter(source)

    @Test
    fun caretRevealsOnlyItsLineWithoutChangingSource() {
        val source = "# Title\n**bold** and *italic*\n[Guide](guide.md)\n"
        val result = render(source, MarkdownTextRange(12))
        assertEquals("Title\n**bold** and *italic*\nGuide\n", result.text)
        assertEquals("# Title\n**bold** and *italic*\n[Guide](guide.md)\n", source)
        assertEquals(source.indexOf("Guide"), result.transformedToOriginal(result.text.indexOf("Guide")))
    }

    @Test
    fun selectedAndComposingLinesRemainLiteral() {
        val source = "**one**\n*two*\n~~three~~\n"
        assertEquals("**one**\n*two*\nthree\n", render(source, MarkdownTextRange(1, 12)).text)
        assertEquals("**one**\n*two*\nthree\n", render(source, MarkdownTextRange(1), MarkdownTextRange(9, 11)).text)
    }

    @Test
    fun sourceOffsetsStayMonotoneAndWithinBoundsForEveryCaret() {
        val source = "# 한글 😀\r\n**bold** [link](path.md) `code`\r\n\n"
        for (caret in 0..source.length) {
            val result = render(source, MarkdownTextRange(caret))
            val forward = (0..source.length).map(result::originalToTransformed)
            val backward = (0..result.text.length).map(result::transformedToOriginal)
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
        assertEquals("\n# **literal**\n```\n[not a link](x)\n\n    **indented**\n", render(source).text)
    }

    @Test
    fun codeSpansEscapesImagesAndIncompleteSyntaxAreConservative() {
        val source = "`**code**` \\*literal\\* ![image](x.png) [unfinished](x\n"
        assertEquals("**code** *literal* ![image](x.png) [unfinished](x\n", render(source).text)
        assertEquals("**escaped**\n", render("**escaped\\**\n").text)
        assertEquals("``unmatched`\n", render("``unmatched`\n").text)
    }

    @Test
    fun formattingProducesSpansAndDoesNotEatWordUnderscores() {
        val result = render("**bold** *italic* ~~strike~~ __strong__ snake_case_name\n")
        assertEquals("bold italic strike strong snake_case_name\n", result.text)
        assertTrue(result.styles.size >= 4)
    }

    @Test
    fun emptyAndTrailingNewlinesHaveValidEndMappings() {
        for (source in listOf("", "\n", "# Heading\n\n", "~~~\n")) {
            val result = render(source)
            assertEquals(result.text.length, result.originalToTransformed(source.length))
            assertEquals(source.length, result.transformedToOriginal(result.text.length))
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
            val kept = MarkdownLivePresentation(MarkdownTextRange(caret), cache = cache).filter(source)
            val fresh = render(source, MarkdownTextRange(caret))
            assertEquals(fresh.text, kept.text, "caret $caret in $source")
            for (offset in 0..source.length) {
                assertEquals(fresh.originalToTransformed(offset), kept.originalToTransformed(offset))
            }
        }
    }

    @Test
    fun aCaretMoveWithinTheSameLineReusesTheWholeResult() {
        val cache = MarkdownLiveCache()
        val source = "**one**\n*two*\n"
        val first = MarkdownLivePresentation(MarkdownTextRange(1), cache = cache).filter(source)
        val second = MarkdownLivePresentation(MarkdownTextRange(4), cache = cache).filter(source)
        assertTrue(first === second)
    }
}
