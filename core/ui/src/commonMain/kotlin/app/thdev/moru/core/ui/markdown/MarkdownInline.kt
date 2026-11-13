package app.thdev.moru.core.ui.markdown

/** Inline syntax remains shared; the renderer retains its original value contract. */
fun markdownRuns(text: String): List<MarkdownRun> =
    app.thdev.moru.core.domain.markdown.markdownRuns(text).map {
        MarkdownRun(it.text,it.bold,it.italic,it.code,it.target,it.imageSource)
    }
