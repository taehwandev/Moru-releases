package app.thdev.moru.feature.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import app.thdev.moru.core.ui.markdown.MarkdownBlock
import app.thdev.moru.core.ui.markdown.MarkdownRun
import app.thdev.moru.core.ui.markdown.markdownRuns
import app.thdev.moru.core.ui.markdown.parseMarkdownBlocks

class MarkdownTest {
    @Test
    fun parsesHeadingsBulletsAndParagraphs() {
        val blocks = parseMarkdownBlocks(
            """
            # Title

            Intro line one
            still the same paragraph

            - first
            - second
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                MarkdownBlock.Heading(1, "Title"),
                MarkdownBlock.Paragraph("Intro line one still the same paragraph"),
                MarkdownBlock.ListItem("first"),
                MarkdownBlock.ListItem("second"),
            ),
            blocks,
        )
    }

    @Test
    fun capturesFencedCodeVerbatim() {
        val blocks = parseMarkdownBlocks(
            """
            before

            ```
            val x = 1
            val y = 2
            ```
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph("before"),
                MarkdownBlock.CodeBlock("val x = 1\nval y = 2"),
            ),
            blocks,
        )
    }

    @Test
    fun headingLevelReflectsHashCount() {
        val blocks = parseMarkdownBlocks("### Deep")

        assertEquals(listOf(MarkdownBlock.Heading(3, "Deep")), blocks)
    }

    @Test
    fun emphasisSplitsBoldItalicAndCodeRuns() {
        val runs = markdownRuns("a **bold** and *italic* and `code` end")

        assertEquals(
            listOf(
                MarkdownRun("a "),
                MarkdownRun("bold", bold = true),
                MarkdownRun(" and "),
                MarkdownRun("italic", italic = true),
                MarkdownRun(" and "),
                MarkdownRun("code", code = true),
                MarkdownRun(" end"),
            ),
            runs,
        )
    }

    @Test
    fun underscoreIsItalic() {
        assertEquals(listOf(MarkdownRun("word", italic = true)), markdownRuns("_word_"))
    }

    @Test
    fun plainTextIsASingleRun() {
        assertEquals(listOf(MarkdownRun("nothing special")), markdownRuns("nothing special"))
    }
}
