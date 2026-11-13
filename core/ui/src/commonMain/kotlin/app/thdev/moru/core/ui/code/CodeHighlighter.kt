package app.thdev.moru.core.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.domain.code.CodeHighlightPolicy
import app.thdev.moru.core.model.code.CodeSyntaxToken.Kind

/** Compose styling adapter for the shared whole-file reader scanner. */
fun highlightCode(text: String, language: CodeLanguage, colors: MoruSyntaxColors): AnnotatedString =
    buildAnnotatedString {
        append(text)
        for (token in CodeHighlightPolicy.tokens(text, language.policy)) {
            val color = when (token.kind) {
                Kind.Keyword -> colors.keyword
                Kind.String -> colors.string
                Kind.Number -> colors.number
                Kind.Comment -> colors.comment
                Kind.Annotation -> colors.annotation
                Kind.Type -> colors.type
                Kind.Function -> colors.function
            }
            addStyle(SpanStyle(color = color, fontStyle = if (token.italic) FontStyle.Italic else null), token.start, token.end)
        }
    }
