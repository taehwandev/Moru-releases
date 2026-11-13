package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * Styled text with links or emphasis spans; the spans keep their own look, the rest follows [style] and [color].
 * [inlineContent] draws the small views, such as icons, the text places with `appendInlineContent`.
 */
@Composable
fun MoruAnnotatedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MoruTheme.typography.body,
    color: Color = MoruTheme.colors.textPrimary,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        onTextLayout = onTextLayout,
        inlineContent = inlineContent,
    )
}
