package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.MoruTextField
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
internal fun MarkdownSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    replacement: String,
    onReplacementChange: (String) -> Unit,
    matchCount: Int,
    activeMatch: Int,
    focus: FocusRequester,
    onMove: (Boolean) -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    val replacementFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoruTextField(
                query, onQueryChange, label = "Find",
                modifier = Modifier.weight(1f).testTag("markdown-find").focusRequester(focus).onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) { onMove(it.isShiftPressed); true } else false
                },
            )
            MoruButton("Previous", { onMove(true); focus.requestFocus() }, type = MoruButtonType.GHOST, enabled = matchCount > 0)
            MoruButton("Next", { onMove(false); focus.requestFocus() }, type = MoruButtonType.GHOST, enabled = matchCount > 0)
        }
        MoruTextField(
            replacement, onReplacementChange, label = "Replace with",
            modifier = Modifier.fillMaxWidth().testTag("markdown-replacement").focusRequester(replacementFocus).onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) { onReplace(); true } else false
            },
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            MoruText(
                when {
                    query.isEmpty() -> "Type to find"
                    matchCount == 0 -> "No matches"
                    activeMatch >= 0 -> "${activeMatch + 1} / $matchCount"
                    else -> "$matchCount matches"
                },
                modifier = Modifier.padding(end = 8.dp).testTag("markdown-match-count"),
                color = MoruTheme.colors.textSecondary, style = MoruTheme.typography.caption,
            )
            MoruButton("Replace", { onReplace(); replacementFocus.requestFocus() }, type = MoruButtonType.GHOST, enabled = activeMatch >= 0)
            MoruButton("Replace all", { onReplaceAll(); replacementFocus.requestFocus() }, type = MoruButtonType.GHOST, enabled = matchCount > 0)
            MoruButton("Close", onClose, type = MoruButtonType.GHOST)
        }
    }
}
