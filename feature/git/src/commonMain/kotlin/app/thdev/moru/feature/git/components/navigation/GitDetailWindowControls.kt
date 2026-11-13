package app.thdev.moru.feature.git.components.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * Explicit window actions beside the current changes/commit label. [onClose], when given, closes the chosen commit
 * and brings the working changes back.
 */
@Composable
internal fun GitDetailWindowControls(
    label: String,
    detached: Boolean,
    onDetach: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        MoruText(label, Modifier.weight(1f), style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MoruIconButton(
            onClick = if (detached) onRestore else onDetach,
            description = if (detached) "Restore Git detail pane" else "Detach Git detail pane",
            modifier = Modifier.testTag(if (detached) "git-detail-restore" else "git-detail-detach"),
            size = 36.dp,
        ) {
            MoruIcon(if (detached) MoruIconName.Dock else MoruIconName.Detach)
        }
        if (onClose != null) {
            MoruIconButton(
                onClick = onClose,
                description = "Close commit and show changes",
                modifier = Modifier.testTag("git-detail-close"),
                size = 36.dp,
            ) {
                MoruIcon(MoruIconName.Close)
            }
        }
    }
}
