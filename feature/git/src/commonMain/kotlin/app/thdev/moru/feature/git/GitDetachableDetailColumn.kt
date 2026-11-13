package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruDetachablePane
import app.thdev.moru.feature.git.components.navigation.GitDetailRestoreRail
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
/** A detached detail stays mounted through compact navigation and native-window moves. */
@Composable
internal fun GitDetachableDetailColumn(
    state: GitManagementUiState,
    history: GitHistoryResult?,
    selectedHash: String?,
    workspacePath: String,
    onEvent: (GitManagementEvent) -> Unit,
    commitDiff: CommitDiffResult?,
    openPath: String?,
    onOpenFile: (String) -> Unit,
    detached: Boolean,
    onDetach: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseCommit: (() -> Unit)? = null,
) {
    val detailLabel = if (selectedHash != null) "Commit · ${selectedHash.take(7)}" else "Changes"
    val folder = workspacePath.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifEmpty { workspacePath }
    MoruDetachablePane(
        detached = detached,
        onRestore = onRestore,
        title = "$folder · $detailLabel",
        modifier = modifier,
        placeholder = { GitDetailRestoreRail(onRestore, it) },
        content = { paneModifier ->
            GitDetailColumn(state, history, selectedHash, commitDiff, openPath, onOpenFile, onEvent,
                detailLabel, detached, onDetach, onRestore, paneModifier, onCloseCommit)
        },
    )
}
