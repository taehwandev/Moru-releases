package app.thdev.moru.feature.workbench.sidebar

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

/** Under a closed project, before it is deleted from the list: says that the folder stays, and asks once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClosedProjectForgetConfirm(
    onConfirm: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = workbenchStrings()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        MoruText(strings.forgetClosedWarning, style = MoruTheme.typography.caption, color = MoruTheme.colors.warning)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        ) {
            MoruButton(strings.confirmForgetClosed, onClick = onConfirm, type = MoruButtonType.SECONDARY)
            MoruButton(strings.keepClosed, onClick = onKeep, type = MoruButtonType.GHOST)
        }
    }
}
