package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.model.git.GitWorkingFile

internal fun gitApproval(
    workspacePath: String,
    snapshot: GitRepositorySnapshot,
    action: GitAction,
): GitApproval = GitApproval(
    workspacePath = workspacePath,
    repositoryPath = snapshot.rootPath,
    revision = snapshot.revision,
    branch = gitBranchLabel(snapshot),
    action = action,
    title = gitActionTitle(action),
    details = gitActionDetails(snapshot, action),
)

internal fun gitBranchLabel(snapshot: GitRepositorySnapshot): String {
    val branch = snapshot.branchName
    return when {
        branch != null && snapshot.head == null -> "$branch (no commits yet)"
        branch != null -> branch
        else -> "Detached HEAD ${snapshot.head?.take(8).orEmpty()}".trim()
    }
}

internal fun gitFileLabel(file: GitWorkingFile): String =
    file.originalPath?.let { "$it → ${file.path}" } ?: file.path

internal fun gitLiteralPaths(files: List<GitWorkingFile>): List<String> =
    files.map { it.path }.distinct()

private fun gitActionTitle(action: GitAction): String = when (action) {
    is GitAction.Commit -> "Commit staged changes"
    is GitAction.Fetch -> "Fetch remote"
    is GitAction.Pull -> "Pull with fast-forward only"
    is GitAction.Push -> "Push branch"
    is GitAction.CreateBranch -> "Create local branch"
    is GitAction.SwitchBranch -> "Switch local branch"
    is GitAction.RenameBranch -> "Rename local branch"
    is GitAction.DeleteBranch -> if (action.force) "Delete unmerged local branch" else "Delete local branch"
    is GitAction.DeleteTag -> "Delete local tag"
    is GitAction.RemoveWorktree -> "Remove worktree"
    GitAction.PruneWorktrees -> "Prune missing worktrees"
    is GitAction.MergeBranch -> "Merge with fast-forward only"
    is GitAction.Stage -> "Stage paths"
    is GitAction.Unstage -> "Unstage paths"
    is GitAction.Discard -> "Discard changes"
}

private fun gitActionDetails(snapshot: GitRepositorySnapshot, action: GitAction): List<String> = when (action) {
    is GitAction.Commit -> listOf("Message: ${action.message}", "Commit only the existing index. Unstaged contents stay outside this commit.", "Staged paths:") +
        snapshot.files.filter { it.hasStaged }.map(::gitFileLabel)
    is GitAction.Fetch -> listOf("Remote: ${action.remote}", "Download remote refs and objects. The current branch and working files stay unchanged.")
    is GitAction.Pull -> listOf("Remote: ${action.remote}", "Remote branch: ${action.branch}", "Fast-forward ${snapshot.branchName} from ${action.remote}/${action.branch}. A dirty or divergent branch is rejected.")
    is GitAction.Push -> listOf("Remote: ${action.remote}", "Local branch: ${snapshot.branchName}", "Remote branch: ${action.branch}", "Publish only this local branch to the named remote branch without force.") +
        if (action.setUpstream) listOf("Set ${action.remote}/${action.branch} as this branch's upstream.") else emptyList()
    is GitAction.CreateBranch -> listOf("New local branch: ${action.name}", "Start at: ${action.startPoint ?: "current HEAD"}", if (action.checkout) "Create and switch to this branch." else "Create this branch without switching.")
    is GitAction.SwitchBranch -> listOf("Target local branch: ${action.name}", "Switch the working folder to this branch. Dirty folders and occupied branches are rejected.")
    is GitAction.RenameBranch -> listOf("Local branch: ${action.oldName}", "New name: ${action.newName}", "Rename this local branch; remote branches stay unchanged.")
    is GitAction.DeleteBranch -> if (action.force) {
        listOf("Local branch: ${action.name}", "This branch is not merged into ${snapshot.branchName ?: "HEAD"}. Commits only on it will no longer be on any branch.",
            "Protected and checked-out branches are still refused. Remote branches stay unchanged.")
    } else listOf("Local branch: ${action.name}", "Delete this local branch only if merged, unprotected, and not checked out.")
    is GitAction.DeleteTag -> listOf("Local tag: ${action.name}", "Delete this tag from this repository only. A tag already pushed stays on the remote.")
    is GitAction.RemoveWorktree -> listOf("Worktree folder: ${action.path}",
        "Remove this checkout's folder. Git refuses it if the folder has changes or untracked files. Its branch stays.")
    GitAction.PruneWorktrees -> listOf("Forget these worktrees whose folders are gone:") +
        snapshot.worktrees.filter { it.prunable }.map { it.path }
    is GitAction.MergeBranch -> listOf("Source branch: ${action.name}", "Target branch: ${snapshot.branchName}", "Fast-forward the current branch only. Dirty folders or divergence are rejected.")
    is GitAction.Stage -> listOf("Update the index for these reviewed literal paths:") + action.paths
    is GitAction.Unstage -> listOf("Remove these reviewed paths from the index and keep working files:") + action.paths
    is GitAction.Discard -> listOf("Put these paths back as the last commit has them; new files are deleted. This cannot be undone:") + action.paths
}
