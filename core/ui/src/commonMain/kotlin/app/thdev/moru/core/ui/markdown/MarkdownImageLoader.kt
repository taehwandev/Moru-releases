package app.thdev.moru.core.ui.markdown

/** Injectable so readers and tests can render images without owning file or network IO. */
fun interface MarkdownImageLoader {
    suspend fun load(source: String, imageBasePath: String?): MarkdownImageLoadResult
}
