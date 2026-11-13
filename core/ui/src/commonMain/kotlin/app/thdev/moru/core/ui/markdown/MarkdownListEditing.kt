package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.input.TextFieldValue

internal object MarkdownListEditing {
    fun enter(value: TextFieldValue): TextFieldValue? =
        app.thdev.moru.core.domain.markdown.MarkdownListEditing.enter(value.markdownEditValue())?.markdownUiValue()
    fun indent(value: TextFieldValue,outdent: Boolean): TextFieldValue? =
        app.thdev.moru.core.domain.markdown.MarkdownListEditing.indent(value.markdownEditValue(),outdent)?.markdownUiValue()
}
