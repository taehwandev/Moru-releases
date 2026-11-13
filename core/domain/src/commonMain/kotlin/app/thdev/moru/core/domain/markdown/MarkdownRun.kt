package app.thdev.moru.core.domain.markdown

/** Styled text runs keep code literal and allow emphasis inside link labels. */
data class MarkdownRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val target: String? = null,
    val imageSource: String? = null,
)
