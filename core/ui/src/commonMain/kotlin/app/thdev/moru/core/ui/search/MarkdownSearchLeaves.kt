package app.thdev.moru.core.ui.search

import app.thdev.moru.core.ui.markdown.MarkdownBlock
import app.thdev.moru.core.ui.markdown.parseMarkdownBlocks

/** Shares the reader's block/row/cell identities, including repeated paragraphs and repeated table cells. */
fun markdownSearchLeaves(markdown: String, path: String): List<TextSearchLeaf> = buildList {
    fun blocks(items: List<MarkdownBlock>, parent: String) {
        items.forEachIndexed { index, block ->
            val at = "$parent/block:$index"
            when (block) {
                is MarkdownBlock.Heading -> addAll(inlineSearchLeaves(block.text, at))
                is MarkdownBlock.Paragraph -> addAll(inlineSearchLeaves(block.text, at))
                is MarkdownBlock.ListItem -> addAll(inlineSearchLeaves(block.text, at))
                is MarkdownBlock.CodeBlock -> add(TextSearchLeaf("$at/code", block.text))
                is MarkdownBlock.Quote -> blocks(block.blocks, at)
                is MarkdownBlock.Callout -> blocks(block.blocks, at)
                is MarkdownBlock.Table -> (listOf(block.header) + block.rows).forEachIndexed { row, cells ->
                    // The renderer draws the header's column count; malformed extra cells are not visible.
                    cells.take(block.header.size).forEachIndexed { column, cell ->
                        addAll(inlineSearchLeaves(cell, "$at/row:$row/cell:$column"))
                    }
                }
                MarkdownBlock.Rule -> Unit
            }
        }
    }
    blocks(parseMarkdownBlocks(markdown), path)
}
