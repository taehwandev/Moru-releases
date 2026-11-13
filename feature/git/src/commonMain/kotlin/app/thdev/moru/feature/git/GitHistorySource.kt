package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
/** What the Git screen reads to show history: a folder's commit log, and what one of its commits changed. */
class GitHistorySource(
    val loadHistory: (workspacePath: String) -> GitHistoryResult,
    val loadCommitDiff: (workspacePath: String, hash: String) -> CommitDiffResult,
    val loadWorkingChanges: (workspacePath: String) -> CommitDiffResult = {
        CommitDiffResult.Unavailable("Working changes are unavailable.")
    },
)
