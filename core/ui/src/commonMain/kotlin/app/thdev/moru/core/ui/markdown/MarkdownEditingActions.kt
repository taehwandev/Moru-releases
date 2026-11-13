package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.input.TextFieldValue

/** Compose values adapt to the shared source-edit policy. */
internal object MarkdownEditingActions {
    fun wrap(value: TextFieldValue,marker: String,closing: String=marker): TextFieldValue =
        app.thdev.moru.core.domain.markdown.MarkdownEditingActions.wrap(value.markdownEditValue(),marker,closing).markdownUiValue()
    fun link(value: TextFieldValue): TextFieldValue =
        app.thdev.moru.core.domain.markdown.MarkdownEditingActions.link(value.markdownEditValue()).markdownUiValue()
    fun prefix(value: TextFieldValue,prefix: String): TextFieldValue =
        app.thdev.moru.core.domain.markdown.MarkdownEditingActions.prefix(value.markdownEditValue(),prefix).markdownUiValue()
}
