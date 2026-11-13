package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.model.git.GitWorkingFile
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.history.GitHistoryResult

/**
 * The middle of the Git screen: the commit history, or, while a file is open, that file's diff in its place. The file
 * is one of the working changes when [selectedHash] is null and one of that commit's files otherwise.
 */
@Composable
internal fun GitCenterPane(
    log: GitHistoryResult?,
    files: List<GitWorkingFile>,
    selectedHash: String?,
    onSelectCommit: (String?) -> Unit,
    openPath: String?,
    onCloseFile: () -> Unit,
    commitDiff: CommitDiffResult?,
    workspacePath: String,
    loadWorkingChanges: (String) -> CommitDiffResult,
    refreshKey: Int,
    modifier: Modifier = Modifier,
    branch: String? = null,
    folder: String? = null,
) {
    when {
        openPath == null -> GitHistoryColumn(log, files, selectedHash, onSelectCommit, modifier, branch, folder)
        selectedHash == null -> GitWorkingFileDiff(workspacePath, openPath, loadWorkingChanges, refreshKey, onCloseFile, modifier)
        else -> GitFileDiffPane(
            path = openPath,
            source = "Commit ${selectedHash.take(7)}",
            file = (commitDiff as? CommitDiffResult.Loaded)?.files?.firstOrNull { it.displayPath == openPath },
            message = when (commitDiff) {
                null -> "Loading diff…"
                is CommitDiffResult.Unavailable -> commitDiff.message
                is CommitDiffResult.Loaded -> "This file is not in the commit's loaded diff."
            },
            onClose = onCloseFile,
            modifier = modifier,
        )
    }
}
