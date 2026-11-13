package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.model.git.GitRepositoryState

internal data class GitManagementUiState(
    val repositoryState: GitRepositoryState? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val historyRevision: Int = 0,
    val remote: String = "",
    val remoteBranch: String = "",
    val commitMessage: String = "",
    val selectedBranch: String? = null,
    val newBranchName: String = "",
    val renameBranchName: String = "",
    val approval: GitApproval? = null,
    val feedback: String? = null,
    val feedbackIsFailure: Boolean = false,
) {
    val snapshot: GitRepositorySnapshot? get() = (repositoryState as? GitRepositoryState.Loaded)?.snapshot
    val idle: Boolean get() = !loading && !busy
    val interactive: Boolean get() = idle && approval == null
}
