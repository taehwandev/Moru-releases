package app.thdev.moru.core.domain.markdown

/** Presentation values retain original source ranges; display offsets remain monotone and bounded. */
class MarkdownLiveResult(
    val text: String,
    val hidden: List<MarkdownTextRange>,
    val styles: List<Span>,
    private val forward: IntArray,
    private val backward: IntArray,
) {
    fun originalToTransformed(offset: Int): Int=forward[offset.coerceIn(0,forward.lastIndex)]
    fun transformedToOriginal(offset: Int): Int=backward[offset.coerceIn(0,backward.lastIndex)]
    data class Span(val start: Int,val end: Int,val style: Style)
    data class Style(
        val monospace: Boolean=false,
        val accent: Boolean=false,
        val bold: Boolean=false,
        val italic: Boolean=false,
        val underline: Boolean=false,
        val strike: Boolean=false,
        val fontSize: Int?=null,
    )
}
