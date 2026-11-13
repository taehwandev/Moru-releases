package app.thdev.moru.feature.workbench

/** The selected project adapter remains the filesystem boundary. */
internal fun resolveMarkdownDocumentLink(fromPath: String,target: String,knownPaths: List<String>): String? =
    app.thdev.moru.core.domain.markdown.MarkdownDocumentPolicy.resolveDocumentLink(fromPath,target,knownPaths)
