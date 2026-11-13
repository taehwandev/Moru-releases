package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import app.thdev.moru.core.designsystem.component.MoruAnnotatedText
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.code.CodeLanguage
import app.thdev.moru.core.ui.code.highlightCode
import app.thdev.moru.core.ui.search.TextSearchChild
import app.thdev.moru.core.ui.search.rememberSearchText
import kotlinx.coroutines.delay

@Composable
internal fun MarkdownCodeBlock(block: MarkdownBlock.CodeBlock) {
    val action = LocalCodeBlockAction.current
    val clipboard = LocalClipboardManager.current
    var copyCount by remember(block.text) { mutableStateOf(0) }
    var copied by remember(block.text) { mutableStateOf(false) }
    LaunchedEffect(copyCount) {
        if (copyCount > 0) {
            delay(2_000)
            copied = false
        }
    }
    Column(
        Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Code).padding(MoruTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        DisableSelection {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (block.language.isNotEmpty()) MoruText(block.language, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
                Spacer(Modifier.weight(1f))
                if (action != null) {
                    action(block.text)
                } else {
                    MoruButton(
                        text = if (copied) "Copied" else "Copy code",
                        onClick = {
                            clipboard.setText(AnnotatedString(block.text))
                            copied = true
                            copyCount++
                        },
                        modifier = Modifier.testTag("markdown-copy-code"),
                        type = MoruButtonType.GHOST,
                        size = MoruButtonSize.Small,
                    )
                }
            }
        }
        val syntax = MoruSyntaxColors.of(MoruTheme.colors)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            TextSearchChild("code") {
            val search = rememberSearchText(remember(block.text, block.language, syntax) { highlightCode(block.text, fenceLanguage(block.language), syntax) })
            MoruAnnotatedText(
                text = search.text,
                modifier = search.modifier,
                onTextLayout = search.onTextLayout,
                color = MoruTheme.colors.textPrimary,
                style = MoruTheme.typography.monospace,
            )
            }
        }
    }
}

/** The language a fence names, by its usual name (`kotlin`, `python`) or by its file extension (`kt`, `py`). */
private fun fenceLanguage(name: String): CodeLanguage =
    CodeLanguage.valueOf(app.thdev.moru.core.domain.markdown.MarkdownCodeLanguage.of(name).name)
