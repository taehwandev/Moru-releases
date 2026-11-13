package app.thdev.moru.core.domain.markdown

/**
 * Parses text that grows at its end, as an answer that streams in. A blank line outside a code fence ends every block,
 * so the blocks before the last such line are kept and only the text after it is read again. Text that does not start
 * with what was kept is read from the beginning. A finished text should still go through [parseMarkdownBlocks], which
 * reads the edge cases this split does not, such as a fence written inside a table row.
 */
class MarkdownStreamParser {
    private var settled = ""
    private var settledBlocks = emptyList<MarkdownBlock>()
    private var settledSteps = 0

    fun parse(text: String): List<MarkdownBlock> {
        if (!text.startsWith(settled)) {
            settled = ""
            settledBlocks = emptyList()
            settledSteps = 0
        }
        val boundary = settledBoundary(text, settled.length)
        if (boundary > settled.length) {
            val next = parseMarkdownBlocks(text.substring(settled.length, boundary), settledSteps)
            settledBlocks = settledBlocks + next
            settledSteps += next.sumOf(::stepCount)
            settled = text.substring(0, boundary)
        }
        return settledBlocks + parseMarkdownBlocks(text.substring(settled.length), settledSteps)
    }
}

private fun stepCount(block: MarkdownBlock): Int = when (block) {
    is MarkdownBlock.Callout -> (if (block.kind == MarkdownCalloutKind.Step) 1 else 0) + block.blocks.sumOf(::stepCount)
    is MarkdownBlock.Quote -> block.blocks.sumOf(::stepCount)
    else -> 0
}
