package app.thdev.moru.core.ui.code

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruAnnotatedText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * A file's text read as code: line numbers beside it, colored by the language its [path] names, selectable, and
 * scrolled both ways so long lines keep their shape. Read only. [lineNumber], when given, draws each line's number in
 * the gutter, one line tall, so a caller can make a line's number into a control; files longer than
 * [MaxControlLines] keep the plain gutter.
 */
@Composable
fun CodeFileView(
    content: String,
    path: String,
    modifier: Modifier = Modifier,
    lineNumber: (@Composable (line: Int, width: Dp) -> Unit)? = null,
) {
    val language = CodeLanguage.of(path)
    val syntax = MoruSyntaxColors.of(MoruTheme.colors)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .ckSurface(MoruSurfaceRole.Code)
            .padding(10.dp),
    ) {
        val lines = remember(content) { content.codeLineCount() }
        if (lineNumber != null && lines <= MaxControlLines) {
            val width = codeGutterWidth(lines)
            // Each number is one code line tall, as the code's own paragraph spaces its lines, so they stay level.
            val style = MoruTheme.typography.monospace
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val row = remember(style, density) { with(density) { measurer.measure("0\n0", style).getLineTop(1).toDp() } }
            Column { for (line in 1..lines) Box(Modifier.height(row), contentAlignment = Alignment.CenterEnd) { lineNumber(line, width) } }
        } else CodeLineGutter(lines)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            SelectionContainer {
                MoruAnnotatedText(
                    text = remember(content, language, syntax) { highlightCode(content, language, syntax) },
                    color = MoruTheme.colors.textPrimary,
                    style = MoruTheme.typography.monospace,
                )
            }
        }
    }
}

/** The longest file whose line numbers are drawn one by one; past it one text keeps the gutter light. */
const val MaxControlLines = 4_000
