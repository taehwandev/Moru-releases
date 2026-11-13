package app.thdev.moru.core.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarkdownCalloutParserTest {
    @Test
    fun keyFlowMarkersBecomeTypedCalloutsWithoutLeakingSyntaxIntoTheBody() {
        val markers = listOf("NOTE", "TIP", "IMPORTANT", "WARNING", "CAUTION", "AI", "USER", "BOT", "STEP")
        val kinds = listOf(MarkdownCalloutKind.Note, MarkdownCalloutKind.Tip, MarkdownCalloutKind.Important,
            MarkdownCalloutKind.Warning, MarkdownCalloutKind.Caution, MarkdownCalloutKind.Ai,
            MarkdownCalloutKind.User, MarkdownCalloutKind.Bot, MarkdownCalloutKind.Step)
        markers.zip(kinds).forEach { (marker, kind) ->
            val block = assertIs<MarkdownBlock.Callout>(parseMarkdownBlocks("> [!${marker.lowercase()}]\r\n> **Useful 내용**").single())
            assertEquals(kind, block.kind)
            assertEquals(listOf(MarkdownBlock.Paragraph("**Useful 내용**")), block.blocks)
        }
    }

    @Test
    fun calloutBodyRetainsHeadingsListsCodeTablesAndNestedQuotes() {
        val block = assertIs<MarkdownBlock.Callout>(parseMarkdownBlocks("""
            > [!BOT] Same-line answer
            >
            > ## Details
            > - [x] [Guide](guide.md)
            >
            > ```kotlin
            >   val value = 1
            > ```
            >
            > | Name | Value |
            > | --- | --- |
            > | one | two |
            >
            > > quoted text
        """.trimIndent()).single())
        assertEquals(listOf(
            MarkdownBlock.Paragraph("Same-line answer"),
            MarkdownBlock.Heading(2, "Details"),
            MarkdownBlock.ListItem("[Guide](guide.md)", checked = true),
            MarkdownBlock.CodeBlock("  val value = 1", "kotlin"),
            MarkdownBlock.Table(listOf("Name", "Value"), listOf(listOf("one", "two")), listOf(TableAlignment.Start, TableAlignment.Start)),
            MarkdownBlock.Quote(listOf(MarkdownBlock.Paragraph("quoted text"))),
        ), block.blocks)
    }

    @Test
    fun literalUnknownIncompleteAndNonLeadingMarkersStayVisible() {
        listOf("> [!CUSTOM]\n> body", "> [!USER", "> [!BOT]suffix", "> ordinary quote\n> [!TIP]").forEach { source ->
            assertIs<MarkdownBlock.Quote>(parseMarkdownBlocks(source).single())
        }
        assertEquals(listOf(MarkdownBlock.Paragraph("[!USER] literal")), parseMarkdownBlocks("[!USER] literal"))
        assertEquals(listOf(MarkdownBlock.CodeBlock("> [!USER]\n> literal", "markdown")),
            parseMarkdownBlocks("```markdown\n> [!USER]\n> literal\n```"))
        assertEquals(listOf(MarkdownBlock.Quote(listOf(MarkdownBlock.CodeBlock("[!TIP]", "")))),
            parseMarkdownBlocks("> ```\n> [!TIP]\n> ```"))
    }

    @Test
    fun emptyCalloutsAreTypedAndOrdinaryQuotesRemainOrdinary() {
        assertEquals(listOf(MarkdownBlock.Callout(MarkdownCalloutKind.User, emptyList())), parseMarkdownBlocks("> [!USER]"))
        assertEquals(listOf(MarkdownBlock.Quote(listOf(MarkdownBlock.Paragraph("Normal quote")))), parseMarkdownBlocks("> Normal quote"))
    }

    @Test
    fun stepsCountAcrossCalloutsAndNestedQuotesButResetForANewDocument() {
        val blocks = parseMarkdownBlocks("> [!STEP] first\n\n> [!BOT]\n> > [!STEP] nested\n\n> [!STEP] last")
        assertEquals(1, assertIs<MarkdownBlock.Callout>(blocks[0]).stepNumber)
        assertEquals(2, assertIs<MarkdownBlock.Callout>(assertIs<MarkdownBlock.Callout>(blocks[1]).blocks.single()).stepNumber)
        assertEquals(3, assertIs<MarkdownBlock.Callout>(blocks[2]).stepNumber)
        assertEquals(1, assertIs<MarkdownBlock.Callout>(parseMarkdownBlocks("> [!STEP] new").single()).stepNumber)
    }

    @Test
    fun streamingKeepsFinishedStepNumbersAndMatchesTheCompleteParser() {
        val parser = MarkdownStreamParser()
        val source = "> [!STEP] first\n\n> [!USER] question\n\n> [!STEP]\n> second\n\n> [!BOT] answer"
        source.indices.forEach { end ->
            val prefix = source.substring(0, end + 1)
            assertEquals(parseMarkdownBlocks(prefix), parser.parse(prefix), "prefix length ${end + 1}")
        }
        assertEquals(parseMarkdownBlocks("> [!STEP] replacement"), parser.parse("> [!STEP] replacement"))
    }

    @Test
    fun quotingACalloutRetainsItsKindAndNestedQuotePrefixes() {
        val block = parseMarkdownBlocks("> [!USER]\n> ## Next message\n> > Keep this quote").single()
        assertEquals("> [!USER]\n> ## Next message\n> > Keep this quote", block.plainText())
        assertEquals(listOf(block), parseMarkdownBlocks(block.plainText()))
    }
}
