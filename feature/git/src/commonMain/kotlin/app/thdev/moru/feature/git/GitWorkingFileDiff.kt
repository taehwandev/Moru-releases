package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.thdev.moru.feature.git.changes.rememberWorkingChanges
import app.thdev.moru.core.model.git.diff.CommitDiffResult

/**
 * One uncommitted file's diff in place of the history. Reads the displayed folder's current patch while it is open;
 * opening it performs no Git mutation.
 */
@Composable
internal fun GitWorkingFileDiff(
    workspacePath: String,
    filePath: String,
    load: (String) -> CommitDiffResult,
    refreshKey: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val diff by rememberWorkingChanges(workspacePath, load, refreshKey)
    val result = diff
    GitFileDiffPane(
        path = filePath,
        source = if ((result as? CommitDiffResult.Loaded)?.truncated == true) "Uncommitted · truncated" else "Uncommitted",
        file = (result as? CommitDiffResult.Loaded)?.files?.firstOrNull { it.displayPath == filePath },
        message = when (result) {
            null -> "Loading working diff…"
            is CommitDiffResult.Unavailable -> result.message
            is CommitDiffResult.Loaded -> if (result.truncated) "This file's patch may be outside the loaded portion."
                else "No current patch for this file. Refresh Git to update the file list."
        },
        onClose = onClose,
        modifier = modifier,
    )
}
