package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType

@Composable
internal fun MarkdownEditorToolbar(
    modifier: Modifier,
    onAction: ((TextFieldValue) -> TextFieldValue) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFind: () -> Unit,
) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
        val actions: List<Pair<String, (TextFieldValue) -> TextFieldValue>> = listOf(
            "Bold" to { MarkdownEditingActions.wrap(it, "**") },
            "Italic" to { MarkdownEditingActions.wrap(it, "*") },
            "Strike" to { MarkdownEditingActions.wrap(it, "~~") },
            "Code" to { MarkdownEditingActions.wrap(it, "`") },
            "Link" to MarkdownEditingActions::link,
            "Heading" to { MarkdownEditingActions.prefix(it, "# ") },
            "List" to { MarkdownEditingActions.prefix(it, "- ") },
            "Task" to { MarkdownEditingActions.prefix(it, "- [ ] ") },
        )
        MoruButton("Find", onFind, type = MoruButtonType.GHOST)
        for ((label, action) in actions) MoruButton(label, { onAction(action) }, type = MoruButtonType.GHOST)
        MoruButton("Undo", onUndo, type = MoruButtonType.GHOST)
        MoruButton("Redo", onRedo, type = MoruButtonType.GHOST)
    }
}
