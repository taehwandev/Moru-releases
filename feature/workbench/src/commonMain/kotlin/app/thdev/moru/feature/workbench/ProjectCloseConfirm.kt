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

/** Shown under a Project row when closing it would stop running agents, so that is never a surprise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProjectCloseConfirm(
    runningSessions: Int,
    onConfirm: () -> Unit,
    onKeepOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = workbenchStrings()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
        MoruText(
            text = strings.closeStopsRunning(runningSessions),
            style = MoruTheme.typography.caption,
            color = MoruTheme.colors.warning,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
        ) {
            MoruButton(strings.closeAndStop, onClick = onConfirm, type = MoruButtonType.SECONDARY)
            MoruButton(strings.keepOpen, onClick = onKeepOpen, type = MoruButtonType.GHOST)
        }
    }
}
