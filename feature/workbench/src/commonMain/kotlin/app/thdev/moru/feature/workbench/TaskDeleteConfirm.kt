package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/** Under a Task row, before it is deleted: says what goes and what stays, and asks once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskDeleteConfirm(
    onConfirm: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = workbenchStrings()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        MoruText(strings.deleteTaskWarning, style = MoruTheme.typography.caption, color = MoruTheme.colors.warning)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        ) {
            MoruButton(strings.confirmDeleteTask, onClick = onConfirm, type = MoruButtonType.SECONDARY)
            MoruButton(strings.keepTask, onClick = onKeep, type = MoruButtonType.GHOST)
        }
    }
}
