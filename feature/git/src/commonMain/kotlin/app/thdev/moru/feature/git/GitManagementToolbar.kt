package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.git.components.navigation.GitSyncButtons

@Composable
internal fun GitManagementToolbar(state: GitManagementUiState, workspacePath: String, onEvent: (GitManagementEvent) -> Unit) {
    val snapshot = state.snapshot
    val remoteReady = state.interactive && state.remote in snapshot?.remotes.orEmpty() && snapshot?.operationInProgress == null
    val targets = snapshot?.remoteTargets(state.remote).orEmpty()
    val branchReady = remoteReady && state.remoteBranch in targets && snapshot?.branchName != null && snapshot.head != null
    val pullReady = branchReady && snapshot?.files?.isEmpty() == true &&
        snapshot?.branches.orEmpty().any { it.isRemote && it.name == "${state.remote}/${state.remoteBranch}" }
    BoxWithConstraints(Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Panel).padding(MoruTheme.spacing.small)) {
        val wide = maxWidth >= 720.dp
        // The repository and the branch commits go to, then where that branch syncs and how far apart they are.
        val selectors: @Composable (Modifier) -> Unit = { rowModifier ->
            Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically) {
                GitRepositoryBranchBar(state, onEvent)
                GitRepositoryHeader(snapshot, workspacePath, Modifier.weight(1f))
            }
        }
        // Fetch, Pull and Push follow the branch above: its tracking branch, or the same name on the remote.
        val actions: @Composable () -> Unit = {
            GitSyncButtons(state.remote, state.remoteBranch, state.idle, remoteReady, pullReady, branchReady, onEvent)
        }
        Column(verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall)) {
            if (wide) {
                // Pickers and their tracking line lead; the sync actions close the one row.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically) {
                    selectors(Modifier.weight(1f))
                    actions()
                }
            } else {
                selectors(Modifier.fillMaxWidth())
                actions()
            }
            if (snapshot != null && snapshot.remotes.isEmpty()) {
                MoruText("No remote configured. Local changes and branches are available.", style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary)
            } else if (snapshot != null && snapshot.files.isNotEmpty()) {
                MoruText("Commit or stash changes before pulling.", style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary)
            } else if (branchReady && !pullReady) {
                MoruText("Push publishes ${snapshot?.branchName} to ${state.remote}/${state.remoteBranch}. Fetch updates remote branches.",
                    style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary)
            }
        }
    }
}
