package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruScreen
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitRepositoryState

@Composable
internal fun GitManagementContent(
    state: GitManagementUiState,
    workspacePath: String,
    workspaceBody: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onEvent: (GitManagementEvent) -> Unit,
) {
    MoruScreen(modifier, padding = 12.dp, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GitManagementToolbar(state, workspacePath, onEvent)
        state.feedback?.let {
            MoruText(it, Modifier.testTag("git-feedback"), color = if (state.feedbackIsFailure) MoruTheme.colors.error else MoruTheme.colors.textPrimary)
        }
        if (state.busy || state.loading) {
            MoruText(if (state.busy) "Running Git action…" else "Loading repository…", Modifier.testTag("git-loading"))
        }
        state.snapshot?.operationInProgress?.let {
            MoruText("Git operation in progress: $it. Finish or abort it in Terminal before branch or sync changes.", color = MoruTheme.colors.error)
        }
        val approval = state.approval
        when {
            approval != null -> GitApprovalPanel(approval, state.idle, onEvent, Modifier.weight(1f).fillMaxWidth())
            state.repositoryState is GitRepositoryState.Unavailable -> {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    MoruText(state.repositoryState.message, Modifier.testTag("git-unavailable"), color = MoruTheme.colors.textSecondary)
                }
            }
            state.snapshot != null -> workspaceBody(Modifier.weight(1f).fillMaxWidth())
            else -> Box(Modifier.weight(1f))
        }
    }
}
