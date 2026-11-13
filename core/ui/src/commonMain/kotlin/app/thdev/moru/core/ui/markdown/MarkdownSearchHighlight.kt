package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Keep the active source match visible even while the Find controls own focus. */
internal class MarkdownSearchHighlight(
    private val base: VisualTransformation,
    private val match: TextRange?,
    private val color: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result = base.filter(text)
        val range = match ?: return result
        val start = result.offsetMapping.originalToTransformed(range.min)
        val end = result.offsetMapping.originalToTransformed(range.max)
        if (start == end) return result
        val highlighted = AnnotatedString.Builder(result.text).apply {
            addStyle(SpanStyle(background = color.copy(alpha = 0.25f)), start, end)
        }.toAnnotatedString()
        return TransformedText(highlighted, result.offsetMapping)
    }
}
