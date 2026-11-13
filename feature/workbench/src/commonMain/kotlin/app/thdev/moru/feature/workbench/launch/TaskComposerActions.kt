package app.thdev.moru.feature.workbench.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.i18n.composerStrings

@Composable
internal fun TaskComposerActions(
    isStarting: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
) {
    val strings = composerStrings()
    Column(verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small)) {
        errorMessage?.let {
            MoruText(
                it,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MoruTheme.colors.error,
                style = MoruTheme.typography.body,
            )
        }
        if (isStarting) MoruText(strings.preparingTask, style = MoruTheme.typography.body, color = MoruTheme.colors.textSecondary)
        MoruButton(strings.cancel, onCancel, type = MoruButtonType.GHOST, enabled = !isStarting)
    }
}
