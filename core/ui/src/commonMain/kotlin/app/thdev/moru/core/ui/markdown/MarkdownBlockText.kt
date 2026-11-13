package app.thdev.moru.core.ui.markdown

/** A block as plain Markdown-like text, to quote it elsewhere: a heading keeps its `#`s and an item its bullet. */
fun MarkdownBlock.plainText(): String = when (this) {
    is MarkdownBlock.Heading -> "#".repeat(level) + " " + text
    is MarkdownBlock.Paragraph -> text
    is MarkdownBlock.ListItem -> "  ".repeat(depth) + (number?.let { "$it." } ?: "-") + " " + text
    is MarkdownBlock.CodeBlock -> text
    is MarkdownBlock.Quote -> blocks.joinToString("\n") { "> " + it.plainText() }
    is MarkdownBlock.Callout -> "> [!${kind.marker}]" + blocks.joinToString("", prefix = "") { block ->
        "\n" + block.plainText().lines().joinToString("\n") { "> $it" }
    }
    is MarkdownBlock.Table -> (listOf(header) + rows).joinToString("\n") { it.joinToString(" | ", "| ", " |") }
    MarkdownBlock.Rule -> "---"
}
