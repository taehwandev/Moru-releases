package app.thdev.moru.core.ui.markdown

/** Read-only Markdown structure. Navigation and scrolling belong to callers. */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class ListItem(val text: String, val depth: Int = 0, val number: String? = null, val checked: Boolean? = null) : MarkdownBlock
    data class CodeBlock(val text: String, val language: String = "") : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class Callout(val kind: MarkdownCalloutKind, val blocks: List<MarkdownBlock>, val stepNumber: Int? = null) : MarkdownBlock
    data class Table(val header: List<String>, val rows: List<List<String>>, val alignments: List<TableAlignment>) : MarkdownBlock
    data object Rule : MarkdownBlock
}
