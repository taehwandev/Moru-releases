package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.thdev.moru.core.domain.markdown.MarkdownEditValue
import app.thdev.moru.core.domain.markdown.MarkdownTextRange

internal fun TextRange.markdownEditRange(): MarkdownTextRange=MarkdownTextRange(start,end)
internal fun MarkdownTextRange.markdownUiRange(): TextRange=TextRange(start,end)
internal fun TextFieldValue.markdownEditValue(): MarkdownEditValue=
    MarkdownEditValue(text,selection.markdownEditRange(),composition?.markdownEditRange())
internal fun MarkdownEditValue.markdownUiValue(): TextFieldValue=
    TextFieldValue(text,selection.markdownUiRange(),composition?.markdownUiRange())
