package app.thdev.moru.core.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors

/** Colors code while it is edited. Only colors change, never characters, so the cursor maps one to one. */
class CodeColoring(
    private val language: CodeLanguage,
    private val colors: MoruSyntaxColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlightCode(text.text, language, colors), OffsetMapping.Identity)
}
