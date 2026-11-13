package app.thdev.moru.core.ui.markdown

/** Current Compose reading values; the shared parser owns syntax and structure. */
internal fun markdownUiBlock(block: app.thdev.moru.core.domain.markdown.MarkdownBlock): MarkdownBlock = when (block) {
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.Heading -> MarkdownBlock.Heading(block.level,block.text)
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.Paragraph -> MarkdownBlock.Paragraph(block.text)
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.ListItem -> MarkdownBlock.ListItem(block.text,block.depth,block.number,block.checked)
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.CodeBlock -> MarkdownBlock.CodeBlock(block.text,block.language)
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.Quote -> MarkdownBlock.Quote(block.blocks.map(::markdownUiBlock))
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.Callout -> MarkdownBlock.Callout(MarkdownCalloutKind.valueOf(block.kind.name),block.blocks.map(::markdownUiBlock),block.stepNumber)
    is app.thdev.moru.core.domain.markdown.MarkdownBlock.Table -> MarkdownBlock.Table(block.header,block.rows,block.alignments.map { TableAlignment.valueOf(it.name) })
    app.thdev.moru.core.domain.markdown.MarkdownBlock.Rule -> MarkdownBlock.Rule
}
