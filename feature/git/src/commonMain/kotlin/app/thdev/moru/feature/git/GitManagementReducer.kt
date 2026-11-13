package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitRepositoryState

internal fun GitManagementUiState.afterRead(result: GitRepositoryState): GitManagementUiState {
    val loaded = (result as? GitRepositoryState.Loaded)?.snapshot
    val nextRemote = remote.takeIf { it in loaded?.remotes.orEmpty() }
        ?: loaded?.upstreamRemote?.takeIf { it in loaded?.remotes.orEmpty() }
        ?: loaded?.remotes?.firstOrNull().orEmpty()
    return copy(
        repositoryState = result,
        loading = false,
        busy = false,
        historyRevision = historyRevision + 1,
        approval = null,
        remote = nextRemote,
        remoteBranch = if (snapshot?.branchName == loaded?.branchName &&
            remote == nextRemote && remoteBranch in loaded?.remoteTargets(nextRemote).orEmpty()) remoteBranch
            else loaded?.upstreamBranch?.takeIf { nextRemote == loaded?.upstreamRemote } ?: loaded?.branchName.orEmpty(),
        selectedBranch = selectedBranch?.takeIf { key -> loaded?.branches?.any { it.selectionKey == key } == true }
            ?: loaded?.branches?.firstOrNull { it.isCurrent }?.selectionKey,
    )
}

internal fun GitManagementUiState.edit(event: GitManagementEvent, workspacePath: String): GitManagementUiState = when (event) {
    is GitManagementEvent.SelectRemote -> copy(remote = event.name, remoteBranch = snapshot?.upstreamBranch
        ?.takeIf { event.name == snapshot?.upstreamRemote } ?: snapshot?.branchName.orEmpty())
    is GitManagementEvent.EditRemoteBranch -> copy(remoteBranch = event.name)
    is GitManagementEvent.EditCommitMessage -> copy(commitMessage = event.message)
    is GitManagementEvent.SelectBranch -> copy(selectedBranch = event.key, renameBranchName = "")
    is GitManagementEvent.EditNewBranchName -> copy(newBranchName = event.name)
    is GitManagementEvent.EditRenameBranchName -> copy(renameBranchName = event.name)
    is GitManagementEvent.Propose -> snapshot?.let { copy(approval = gitApproval(workspacePath, it, event.action)) } ?: this
    GitManagementEvent.Cancel -> copy(approval = null)
    else -> this
}
