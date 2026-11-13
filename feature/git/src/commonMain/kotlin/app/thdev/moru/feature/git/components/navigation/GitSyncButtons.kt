package app.thdev.moru.feature.git.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.feature.git.GitManagementEvent

/** Reload the folder's state, then the three remote actions, Push being the one the toolbar leads with. */
@Composable
internal fun GitSyncButtons(
    remote: String, branch: String, idle: Boolean, fetchReady: Boolean,
    pullReady: Boolean, pushReady: Boolean, onEvent: (GitManagementEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall), verticalAlignment = Alignment.CenterVertically) {
        MoruIconButton(MoruIconName.Refresh, "Refresh", { onEvent(GitManagementEvent.Refresh) },
            Modifier.testTag("git-refresh"), enabled = idle)
        MoruButton("Fetch", { onEvent(GitManagementEvent.Propose(GitAction.Fetch(remote))) }, Modifier.testTag("git-fetch"),
            MoruButtonType.SECONDARY, fetchReady, icon = MoruIconName.Fetch)
        MoruButton("Pull", { onEvent(GitManagementEvent.Propose(GitAction.Pull(remote, branch))) }, Modifier.testTag("git-pull"),
            MoruButtonType.SECONDARY, pullReady, icon = MoruIconName.Pull)
        MoruButton("Push", { onEvent(GitManagementEvent.Propose(GitAction.Push(remote, branch))) }, Modifier.testTag("git-push"),
            MoruButtonType.PRIMARY, pushReady, icon = MoruIconName.Push)
    }
}
