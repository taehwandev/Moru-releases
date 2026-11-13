package app.thdev.moru.core.ui.markdown

/** The existing incremental reader delegates parsing and settled state to shared Kotlin. */
internal class MarkdownStreamParser {
    private val shared = app.thdev.moru.core.domain.markdown.MarkdownStreamParser()
    private var previousShared = emptyList<app.thdev.moru.core.domain.markdown.MarkdownBlock>()
    private var previousUi = emptyList<MarkdownBlock>()

    fun parse(text: String): List<MarkdownBlock> {
        val blocks=shared.parse(text)
        val values=blocks.mapIndexed { index,block ->
            if (previousShared.getOrNull(index) === block) previousUi[index] else markdownUiBlock(block)
        }
        previousShared=blocks; previousUi=values
        return values
    }
}

internal fun settledBoundary(text: String, from: Int): Int =
    app.thdev.moru.core.domain.markdown.settledBoundary(text,from)
