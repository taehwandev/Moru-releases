package app.thdev.moru.core.ui.code

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** The line numbers beside code, one per line, as wide as the largest number. */
@Composable
fun CodeLineGutter(lineCount: Int, modifier: Modifier = Modifier) {
    MoruText(
        text = remember(lineCount) { (1..lineCount).joinToString("\n") },
        color = MoruTheme.colors.textSecondary,
        style = MoruTheme.typography.monospace.copy(textAlign = TextAlign.End),
        modifier = modifier.width(codeGutterWidth(lineCount)),
    )
}
