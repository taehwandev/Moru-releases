package app.thdev.moru.core.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownResponseParserTest {
    @Test
    fun plainWebLinksTrimSentencePunctuationButKeepBalancedPathParentheses() {
        val source = "See https://example.org/a_(b). Then (http://example.org)."
        val runs = markdownRuns(source)
        assertEquals(source, runs.joinToString("") { it.text })
        assertEquals(
            listOf(
                MarkdownRun("https://example.org/a_(b)", target = "https://example.org/a_(b)"),
                MarkdownRun("http://example.org", target = "http://example.org"),
            ),
            runs.filter { it.target != null },
        )
    }

    @Test
    fun bareLinksNeverOverrideExplicitLinkLabelsOrInlineCode() {
        assertEquals(
            listOf(
                MarkdownRun("https://example.org", code = true), MarkdownRun(" "),
                MarkdownRun("https://label.example", target = "guide.md"), MarkdownRun(" "),
                MarkdownRun("HTTPS://example.org", target = "HTTPS://example.org"),
            ),
            markdownRuns("`https://example.org` [https://label.example](guide.md) HTTPS://example.org"),
        )
    }

    @Test
    fun underscoresInWebPathsRemainPartOfTheLink() {
        val target = "https://example.org/_assets_/snake_case.png"
        assertEquals(listOf(MarkdownRun(target, target = target)), markdownRuns(target))
    }

    @Test
    fun markdownImagesKeepAltTextAndLocalRemoteAndFileDestinations() {
        assertEquals(
            listOf(
                MarkdownRun("Before "),
                MarkdownRun("Mockup", imageSource = "images/ui%20preview.png"),
                MarkdownRun(" after "),
                MarkdownRun("", imageSource = "https://example.org/image.png"),
                MarkdownRun(" "),
                MarkdownRun("Local", imageSource = "file:///tmp/ui%20preview.png"),
            ),
            markdownRuns("Before ![Mockup](images/ui%20preview.png \"Preview\") after ![](https://example.org/image.png) ![Local](file:///tmp/ui%20preview.png)"),
        )
    }

    @Test
    fun imageDestinationsAllowEscapedSpacesAnglesAndBalancedParentheses() {
        assertEquals(
            listOf(
                MarkdownRun("A", imageSource = "my image.png"), MarkdownRun(" "),
                MarkdownRun("B", imageSource = "images/preview(2).png"), MarkdownRun(" "),
                MarkdownRun("C", imageSource = "my second image.png"),
            ),
            markdownRuns("![A](<my image.png>) ![B](images/preview(2).png) ![C](my\\ second\\ image.png)"),
        )
    }

    @Test
    fun codeContainingImagesAndLinksRemainsLiteral() {
        val source = "![mockup](image.png) https://example.org"
        assertEquals(listOf(MarkdownRun(source, code = true)), markdownRuns("`$source`"))
        assertEquals(listOf(MarkdownBlock.CodeBlock(source, "markdown")), parseMarkdownBlocks("```markdown\n$source\n```"))
    }
}
