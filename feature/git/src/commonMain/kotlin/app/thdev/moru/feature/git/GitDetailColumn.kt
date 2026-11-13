package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.feature.git.components.navigation.GitDetailWindowControls
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult

/**
 * The right column: the changes to stage and commit while the working folder is selected, and the files the chosen
 * commit changed once a commit is. A file chosen in either list opens its diff in place of the history ([onOpenFile]),
 * so the list stays here to pick the next one. [openPath] is the file whose diff is open.
 */
@Composable
internal fun GitDetailColumn(
    state: GitManagementUiState,
    history: GitHistoryResult?,
    selectedHash: String?,
    commitDiff: CommitDiffResult?,
    openPath: String?,
    onOpenFile: (String) -> Unit,
    onEvent: (GitManagementEvent) -> Unit,
    detailLabel: String,
    detached: Boolean,
    onDetach: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseCommit: (() -> Unit)? = null,
) {
    val commit = (history as? GitHistoryResult.Loaded)?.commits?.firstOrNull { it.hash == selectedHash }
    Column(modifier.ckSurface(MoruSurfaceRole.Panel).clipToBounds()) {
        GitDetailWindowControls(detailLabel, detached, onDetach, onRestore,
            onClose = onCloseCommit?.takeIf { selectedHash != null })
        when {
            selectedHash == null -> GitChangesPanel(
                state, onEvent, Modifier.weight(1f).padding(10.dp), onOpenFile, openPath,
            )
            commit != null -> GitCommitFilesPanel(commit, commitDiff, openPath, onOpenFile, Modifier.weight(1f))
            else -> MoruText(
                if (history == null) "Loading commit…" else "Selected commit is unavailable. Refresh Git history.",
                Modifier.weight(1f).padding(16.dp),
            )
        }
    }
}
