package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.MoruLinearProgress

/** Stateless, opaque and compact so the message stays readable over any workbench surface. */
@Composable
internal fun UpdateSnackbarContent(
    status: UpdateStatus,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 420.dp,
) {
    val strings = appStrings()
    val text = updateMessage(strings, status) ?: return
    val colors = MoruTheme.colors
    val shape = RoundedCornerShape(MoruTheme.radius.large)
    val accent = when (status) {
        is UpdateStatus.Failed -> colors.warning
        is UpdateStatus.Ready, is UpdateStatus.UpToDate -> colors.primary
        else -> colors.textSecondary
    }
    Column(
        modifier.testTag("update-snackbar").semantics { liveRegion = LiveRegionMode.Polite }
            .width(width).shadow(12.dp, shape).clip(shape).background(colors.surface)
            .border(1.dp, colors.border, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MoruIcon(
                name = when (status) {
                    is UpdateStatus.Ready -> MoruIconName.Reopen
                    is UpdateStatus.UpToDate -> MoruIconName.Check
                    is UpdateStatus.Failed -> MoruIconName.Flag
                    else -> MoruIconName.Download
                },
                tint = accent, modifier = Modifier.size(20.dp),
            )
            MoruText(text, modifier = Modifier.weight(1f), style = MoruTheme.typography.body, color = colors.textPrimary)
        }
        if (status is UpdateStatus.Downloading) {
            MoruLinearProgress(progress = status.percent.coerceIn(0, 100) / 100f, modifier = Modifier.fillMaxWidth(), color = accent)
        }
        if (status is UpdateStatus.Ready || status is UpdateStatus.Failed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (status is UpdateStatus.Ready) {
                    MoruButton(strings.updateLater, onDismiss, type = MoruButtonType.GHOST)
                    MoruButton(strings.updateRestart, onRestart)
                } else MoruButton(strings.dismiss, onDismiss, type = MoruButtonType.GHOST)
            }
        }
    }
}
