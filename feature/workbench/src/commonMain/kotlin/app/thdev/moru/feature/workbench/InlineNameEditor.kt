package app.thdev.moru.feature.workbench

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruCompactTextField
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/**
 * Edits a name in place of the name, for a Project or a Task. Enter or the check saves, Escape or the cross leaves it
 * as it was; a blank name or an unchanged one cannot be saved. [tag] names the field for tests.
 */
@Composable
internal fun InlineNameEditor(
    initial: String,
    tag: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = workbenchStrings()
    var draft by remember(initial) { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val canSave = draft.isNotBlank() && draft.trim() != initial
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruCompactTextField(
            value = draft,
            onValueChange = { draft = it.replace("\n", " ") },
            interactionSource = interaction,
            modifier = Modifier.weight(1f).testTag(tag).focusRequester(focus).onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter -> { if (canSave) onSave(draft.trim()) else onCancel(); true }
                    Key.Escape -> { onCancel(); true }
                    else -> false
                }
            },
        )
        MoruIconButton(
            icon = MoruIconName.Check,
            description = strings.saveProjectName,
            onClick = { if (canSave) onSave(draft.trim()) },
            type = if (canSave) MoruButtonType.PRIMARY else MoruButtonType.GHOST,
            enabled = canSave,
            size = MoruButtonSize.ExtraSmall,
        )
        MoruIconButton(
            icon = MoruIconName.Close,
            description = strings.cancelRename,
            onClick = onCancel,
            type = MoruButtonType.GHOST,
            size = MoruButtonSize.ExtraSmall,
        )
    }
}
