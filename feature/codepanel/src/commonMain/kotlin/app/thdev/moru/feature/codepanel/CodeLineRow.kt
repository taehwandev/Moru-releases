package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruAnnotatedText
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

private const val ChangedTintAlpha = 0.16f

/**
 * One line of code with its number. A changed line carries a + marker as well as its tint, so it is not told apart by
 * color alone. Long lines wrap, the way the diff view reads, so a narrow panel still shows them whole.
 */
@Composable
internal fun CodeLineRow(number: Int, text: AnnotatedString, changed: Boolean, modifier: Modifier = Modifier) {
    val colors = MoruTheme.colors
    val style = MoruTheme.typography.monospace
    Row(
        modifier.fillMaxWidth()
            .background(if (changed) colors.success.copy(alpha = ChangedTintAlpha) else Color.Transparent)
            .padding(horizontal = 8.dp),
    ) {
        MoruText(
            number.toString(),
            style = style.copy(textAlign = TextAlign.End),
            color = colors.textSecondary,
            modifier = Modifier.width(44.dp).padding(end = 8.dp),
        )
        MoruText(if (changed) "+" else " ", style = style, modifier = Modifier.width(14.dp))
        MoruAnnotatedText(text.ifEmpty { AnnotatedString(" ") }, style = style, modifier = Modifier.weight(1f))
    }
}
