package app.thdev.moru.feature.git.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.feature.git.i18n.historyStrings

/** Loads and shows the diff for [commit]. With no loader wired it says so, without reading anything. */
@Composable
internal fun SelectedCommitDiff(
    commit: GitCommit,
    loadCommitDiff: ((String) -> CommitDiffResult)?,
    modifier: Modifier = Modifier,
) {
    if (loadCommitDiff == null) {
        CommitDiffPane(
            commit = commit,
            diff = CommitDiffResult.Unavailable(historyStrings().diffNotConnected),
            modifier = modifier,
        )
        return
    }
    val diff by rememberCommitDiff(commit.hash, loadCommitDiff)
    CommitDiffPane(commit = commit, diff = diff, modifier = modifier)
}
