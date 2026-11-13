package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.input.TextFieldValue

internal class MarkdownEditHistory(initial: TextFieldValue) {
    private val history=app.thdev.moru.core.domain.markdown.MarkdownEditHistory(initial.markdownEditValue())
    val value: TextFieldValue get()=history.value.markdownUiValue()
    fun accept(next: TextFieldValue): TextFieldValue = history.accept(next.markdownEditValue()).markdownUiValue()
    fun undo(): TextFieldValue = history.undo().markdownUiValue()
    fun redo(): TextFieldValue = history.redo().markdownUiValue()
}
