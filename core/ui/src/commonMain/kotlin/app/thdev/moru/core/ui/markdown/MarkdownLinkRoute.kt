package app.thdev.moru.core.ui.markdown

/** Current host routing keeps its callback and enum while sharing the decision. */
fun markdownLinkKind(target: String,isLocalPage: (String) -> Boolean = { false }): MarkdownLinkKind? =
    app.thdev.moru.core.domain.markdown.MarkdownLinkPolicy.kind(target,isLocalPage)?.let(MarkdownLinkKind::valueOf)
