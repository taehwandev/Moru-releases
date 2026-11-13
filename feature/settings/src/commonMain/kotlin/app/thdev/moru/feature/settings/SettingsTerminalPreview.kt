package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.terminal.TerminalColorScheme
import app.thdev.moru.core.ui.terminal.terminalHexColor

/** A few lines drawn in [scheme], so a change is seen before opening a terminal. */
@Composable
internal fun SettingsTerminalPreview(scheme: TerminalColorScheme) {
    val foreground = terminalHexColor(scheme.foreground) ?: Color.Unspecified
    fun ansi(index: Int) = terminalHexColor(scheme.ansi.getOrNull(index)) ?: foreground
    val shape = RoundedCornerShape(MoruTheme.radius.xSmall)
    val style = MoruTheme.typography.monospace.copy(color = foreground)
    Column(
        modifier = Modifier.fillMaxWidth().background(terminalHexColor(scheme.background) ?: Color.Unspecified, shape)
            .border(1.dp, MoruTheme.colors.border, shape).padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("settings-terminal-preview"),
    ) {
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = ansi(2))) { append("~/project") }
                withStyle(SpanStyle(color = ansi(4))) { append(" main") }
                append(" $ ls")
            },
            style = style,
        )
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = ansi(4))) { append("src ") }
                withStyle(SpanStyle(color = ansi(6))) { append("docs ") }
                withStyle(SpanStyle(color = ansi(5))) { append("build.gradle ") }
                withStyle(SpanStyle(color = ansi(3))) { append("README.md") }
            },
            style = style,
        )
        BasicText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = ansi(1))) { append("error ") }
                withStyle(SpanStyle(color = ansi(8))) { append("dim ") }
                withStyle(SpanStyle(background = terminalHexColor(scheme.selection) ?: Color.Unspecified)) { append("selected") }
            },
            style = style,
        )
    }
}
