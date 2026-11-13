package app.thdev.moru.core.ui.markdown

/** Existing UI values around the shared Markdown parser. */
fun parseMarkdownBlocks(text: String): List<MarkdownBlock> =
    app.thdev.moru.core.domain.markdown.parseMarkdownBlocks(text).map(::markdownUiBlock)
