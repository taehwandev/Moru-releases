package app.thdev.moru.core.domain.markdown

/** Source, directional selection and optional in-progress composition, without a UI dependency. */
data class MarkdownEditValue(
    val text: String,
    val selection: MarkdownTextRange=MarkdownTextRange.Zero,
    val composition: MarkdownTextRange?=null,
)
