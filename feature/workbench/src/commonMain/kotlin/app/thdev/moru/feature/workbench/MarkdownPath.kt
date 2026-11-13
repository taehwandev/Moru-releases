package app.thdev.moru.feature.workbench

fun isMarkdownPath(path: String): Boolean =
    app.thdev.moru.core.domain.markdown.MarkdownDocumentPolicy.isMarkdownPath(path)
