package app.thdev.moru.core.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParserTest {
    @Test
    fun languageFencesPreserveWhitespaceAndClosingFenceLength() {
        assertEquals(
            listOf(MarkdownBlock.CodeBlock("  val a = 1\n```\n", "kotlin")),
            parseMarkdownBlocks("````kotlin\n  val a = 1\n```\n\n````"),
        )
    }

    @Test
    fun unterminatedFenceKeepsItsContent() {
        assertEquals(listOf(MarkdownBlock.CodeBlock("# literal", "sh")), parseMarkdownBlocks("~~~sh\n# literal"))
    }

    @Test
    fun orderedNestedAndTaskItemsKeepTheirMeaning() {
        assertEquals(
            listOf(
                MarkdownBlock.ListItem("first", number = "3"),
                MarkdownBlock.ListItem("pending", depth = 1, checked = false),
                MarkdownBlock.ListItem("done", depth = 1, checked = true),
            ),
            parseMarkdownBlocks("3. first\n  - [ ] pending\n  - [x] done"),
        )
    }

    @Test
    fun quoteRetainsBlocksAndRuleIsNotAListItem() {
        assertEquals(
            listOf(MarkdownBlock.Quote(listOf(MarkdownBlock.Heading(2, "Note"), MarkdownBlock.Paragraph("quoted text"))), MarkdownBlock.Rule),
            parseMarkdownBlocks("> ## Note\n> quoted text\n\n---"),
        )
    }

    @Test
    fun tableHandlesAlignmentEscapedPipesAndCode() {
        assertEquals(
            listOf(MarkdownBlock.Table(
                header = listOf("Name", "Value"),
                rows = listOf(listOf("a | b", "`x|y`"), listOf("short", "")),
                alignments = listOf(TableAlignment.Start, TableAlignment.End),
            )),
            parseMarkdownBlocks("| Name | Value |\n| :--- | ---: |\n| a \\| b | `x|y` |\n| short |"),
        )
    }

    @Test
    fun linksAndNestedEmphasisKeepDestinationsWhileCodeRemainsLiteral() {
        assertEquals(
            listOf(
                MarkdownRun("bold", bold = true, target = "guide.md"), MarkdownRun(" "),
                MarkdownRun("Label", target = "Page"), MarkdownRun(" "),
                MarkdownRun("***literal***", code = true), MarkdownRun(" "),
                MarkdownRun("both", bold = true, italic = true),
            ),
            markdownRuns("[**bold**](guide.md) [[Page|Label]] `***literal***` ***both***"),
        )
    }

    @Test
    fun webLinksAreRecognizedAndUnderscoresInsideIdentifiersStayLiteral() {
        assertEquals(
            listOf(MarkdownRun("https://example.org", target = "https://example.org"), MarkdownRun(" snake_case_name")),
            markdownRuns("<https://example.org> snake_case_name"),
        )
    }
}
