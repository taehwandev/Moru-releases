package app.thdev.moru.core.domain.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownPolicyTest {
    @Test
    fun fencedLanguageAliasesAndImageBudgetsRemainSourceExact() {
        assertEquals("Script",MarkdownCodeLanguage.of(" TypeScript ").name)
        assertEquals("Swift",MarkdownCodeLanguage.of("objective-c").name)
        assertEquals("Shell",MarkdownCodeLanguage.of("console").name)
        assertEquals("Plain",MarkdownCodeLanguage.of("unknown").name)
        assertEquals(8*1024*1024,MarkdownImagePolicy.maximumEncodedBytes)
        assertEquals(24_000_000L,MarkdownImagePolicy.maximumSourcePixels)
        assertEquals(1600,MarkdownImagePolicy.maximumDecodedDimension)
        assertEquals("Web",MarkdownImagePolicy.sourceKind("HTTP://localhost/image.png"))
        assertEquals("File",MarkdownImagePolicy.sourceKind("file:///tmp/image.png"))
        assertEquals("Path",MarkdownImagePolicy.sourceKind("../image.png"))
        assertNull(MarkdownImagePolicy.sourceKind("data:image/png;base64,a"))
        assertNull(MarkdownImagePolicy.sourceKind("C:/image.png"))
        assertNull(MarkdownImagePolicy.sourceKind(" "))
        assertNull(MarkdownImagePolicy.sourceKind("x".repeat(4097)))
    }
    @Test
    fun blocksRetainNestedStepsListsAndLiteralFences() {
        val blocks=parseMarkdownBlocks("# 검증 😀\n\n> [!STEP] First\n> > [!STEP] Nested\n\n3. first\n  - [x] done\n\n````kotlin\n  val a = 1\n```\n````")
        assertEquals(MarkdownBlock.Heading(1,"검증 😀"),blocks[0])
        val step=blocks[1] as MarkdownBlock.Callout
        assertEquals(1,step.stepNumber)
        assertEquals(2,(step.blocks[1] as MarkdownBlock.Callout).stepNumber)
        assertEquals(MarkdownBlock.ListItem("first",number="3"),blocks[2])
        assertEquals(MarkdownBlock.ListItem("done",depth=1,checked=true),blocks[3])
        assertEquals(MarkdownBlock.CodeBlock("  val a = 1\n```","kotlin"),blocks[4])
    }
    @Test
    fun tableEscapedPipesAlignmentAndShortRowsRetainSourceMeaning() {
        val table=parseMarkdownBlocks("| Name | Value |\n| :--- | ---: |\n| a \\| b | `x|y` |\n| short | ").single() as MarkdownBlock.Table
        assertEquals(listOf("a | b","`x|y`"),table.rows[0])
        assertEquals(listOf("short",""),table.rows[1])
        assertEquals(listOf(MarkdownTableAlignment.Start,MarkdownTableAlignment.End),table.alignments)
    }
    @Test
    fun inlineRunsRetainLiteralCodeLabelsAndImages() {
        val runs=markdownRuns("**bold** `a*b` [[doc|*label*]] ![alt](image.png)")
        assertTrue(runs.any { it.text=="bold" && it.bold })
        assertTrue(runs.any { it.text=="a*b" && it.code && !it.italic })
        assertTrue(runs.any { it.text=="label" && it.italic && it.target=="doc" })
        assertTrue(runs.any { it.text=="alt" && it.imageSource=="image.png" })
    }
    @Test
    fun streamingPreservesSettledStepNumbersAndResetsReplacement() {
        val source="> [!STEP] first\n\n> [!STEP] second\n\n```\nx\n\nmore\n```"
        val stream=MarkdownStreamParser()
        for (end in source.indices) assertEquals(parseMarkdownBlocks(source.take(end+1)),stream.parse(source.take(end+1)))
        assertEquals(parseMarkdownBlocks("replacement"),stream.parse("replacement"))
    }
    @Test
    fun documentPathsAndLinksRetainProjectBoundaryAndExtensionRules() {
        assertTrue(MarkdownDocumentPolicy.isMarkdownPath("folder/a.MARKDOWN"))
        assertFalse(MarkdownDocumentPolicy.isMarkdownPath("folder/a.md "))
        assertEquals("notes/검증.md",MarkdownDocumentPolicy.resolveDocumentLink("notes/index.md","%EA%B2%80%EC%A6%9D",listOf("notes/검증.md")))
        assertNull(MarkdownDocumentPolicy.resolveDocumentLink("index.md","../outside",emptyList()))
        assertNull(MarkdownDocumentPolicy.resolveDocumentLink("index.md","https://example.invalid",emptyList()))
        assertEquals("File",MarkdownLinkPolicy.kind("notes/doc.md"))
        assertEquals("Web",MarkdownLinkPolicy.kind("https://example.invalid"))
        assertEquals("LocalPage",MarkdownLinkPolicy.kind("http://127.0.0.1") { true })
        assertNull(MarkdownLinkPolicy.kind("#heading"))
        assertNull(MarkdownLinkPolicy.kind("javascript:alert(1)"))
    }
    @Test
    fun tableAllocationKeepsShortColumnsAndScrollsAtFloor() {
        val columns=listOf(MarkdownTableLayout.Column(30,50),MarkdownTableLayout.Column(50,150))
        assertEquals(listOf(50,150),MarkdownTableLayout.widths(columns,300,64))
        assertEquals(listOf(33,67),MarkdownTableLayout.widths(columns,100,64))
        assertEquals(listOf(30,50),MarkdownTableLayout.widths(columns,70,64))
    }
}
