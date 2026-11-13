package app.thdev.moru.feature.git

import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitWorktreeInfo

/**
 * One icon per kind of branch, the same in the branch picker, the branch list and the Code panel: a house for the
 * branch checked out in the repository's main checkout, trees for one checked out in a linked worktree, a cloud for a
 * remote branch and a branch mark for any other local branch.
 */
internal fun gitBranchIcon(branch: GitBranchInfo, worktrees: List<GitWorktreeInfo>): MoruIconName {
    if (branch.isRemote) return MoruIconName.Cloud
    val worktree = worktrees.firstOrNull { it.branch == branch.name }
    return when {
        worktree?.isMain == true -> MoruIconName.Home
        worktree != null -> MoruIconName.Worktree
        else -> MoruIconName.Branch
    }
}

/** The house or the trees for one checkout, matching [gitBranchIcon]. */
internal fun gitWorktreeIcon(worktree: GitWorktreeInfo): MoruIconName =
    if (worktree.isMain) MoruIconName.Home else MoruIconName.Worktree
