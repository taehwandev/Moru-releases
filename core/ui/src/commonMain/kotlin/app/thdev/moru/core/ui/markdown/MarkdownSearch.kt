package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal object MarkdownSearch {
    fun matches(text: String,query: String): List<TextRange> =
        app.thdev.moru.core.domain.markdown.MarkdownSearch.matches(text,query).map { it.markdownUiRange() }
    fun next(matches: List<TextRange>,selection: TextRange,backwards: Boolean=false): TextRange? =
        app.thdev.moru.core.domain.markdown.MarkdownSearch.next(matches.map { it.markdownEditRange() },selection.markdownEditRange(),backwards)?.markdownUiRange()
    fun replaceCurrent(value: TextFieldValue,query: String,replacement: String): TextFieldValue =
        app.thdev.moru.core.domain.markdown.MarkdownSearch.replaceCurrent(value.markdownEditValue(),query,replacement).markdownUiValue()
    fun replaceAll(value: TextFieldValue,query: String,replacement: String): TextFieldValue =
        app.thdev.moru.core.domain.markdown.MarkdownSearch.replaceAll(value.markdownEditValue(),query,replacement).markdownUiValue()
}
