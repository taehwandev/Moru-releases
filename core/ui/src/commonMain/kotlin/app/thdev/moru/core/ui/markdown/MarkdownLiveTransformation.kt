package app.thdev.moru.core.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import app.thdev.moru.core.domain.markdown.MarkdownLivePresentation
import app.thdev.moru.core.domain.markdown.MarkdownLiveResult

/** Compose styling and display offsets adapt the shared source presentation. */
internal class MarkdownLiveTransformation(
    private val selection: TextRange,
    private val composition: TextRange?=null,
    private val accent: Color=Color.Unspecified,
    private val cache: MarkdownLiveCache=MarkdownLiveCache(),
): VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result=MarkdownLivePresentation(selection.markdownEditRange(),composition?.markdownEditRange(),cache.shared).filter(text.text)
        cache.previous(result,accent)?.let { return it }
        val rendered=AnnotatedString.Builder(result.text)
        for (span in result.styles) {
            val start=result.originalToTransformed(span.start)
            val end=result.originalToTransformed(span.end)
            if (start<end) with(span.style) {
                rendered.addStyle(SpanStyle(
                    fontFamily=if (monospace) FontFamily.Monospace else null,
                    color=if (this.accent) this@MarkdownLiveTransformation.accent else Color.Unspecified,
                    fontWeight=if (bold) FontWeight.Bold else null,
                    fontStyle=if (italic) FontStyle.Italic else null,
                    fontSize=fontSize?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
                    textDecoration=if (underline) TextDecoration.Underline else if (strike) TextDecoration.LineThrough else null,
                ),start,end)
            }
        }
        val transformed=TransformedText(rendered.toAnnotatedString(),object: OffsetMapping {
            override fun originalToTransformed(offset: Int): Int=result.originalToTransformed(offset)
            override fun transformedToOriginal(offset: Int): Int=result.transformedToOriginal(offset)
        })
        cache.keep(result,accent,transformed)
        return transformed
    }
}

/** Preserves whole-result identity on same-line caret moves. */
internal class MarkdownLiveCache {
    val shared=app.thdev.moru.core.domain.markdown.MarkdownLiveCache()
    private var result: MarkdownLiveResult?=null
    private var accent: Color?=null
    private var transformed: TransformedText?=null
    fun previous(result: MarkdownLiveResult,accent: Color): TransformedText?=
        transformed.takeIf { result===this.result && accent==this.accent }
    fun keep(result: MarkdownLiveResult,accent: Color,transformed: TransformedText) {
        this.result=result;this.accent=accent;this.transformed=transformed
    }
}
