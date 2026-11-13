package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitAction

internal sealed interface GitManagementEvent {
    data object Refresh : GitManagementEvent
    data object Confirm : GitManagementEvent
    data object Cancel : GitManagementEvent
    data class SelectRemote(val name: String) : GitManagementEvent
    data class EditRemoteBranch(val name: String) : GitManagementEvent
    data class EditCommitMessage(val message: String) : GitManagementEvent
    data class SelectBranch(val key: String) : GitManagementEvent
    data class EditNewBranchName(val name: String) : GitManagementEvent
    data class EditRenameBranchName(val name: String) : GitManagementEvent
    data class Propose(val action: GitAction) : GitManagementEvent
    data class ChangeIndex(val action: GitAction, val revision: String) : GitManagementEvent
    /** Show the worktree at [path] in Git instead; the screen's owner decides which folder Git is on. */
    data class OpenWorktree(val path: String) : GitManagementEvent
}
