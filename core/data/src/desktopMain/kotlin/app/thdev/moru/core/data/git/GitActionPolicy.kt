package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitRepositorySnapshot

internal class GitActionPolicy(private val runner: GitProcessRunner) {
    fun rejection(context: GitRepositoryDiscovery, snapshot: GitRepositorySnapshot, action: GitAction): String? {
        if (snapshot.operationInProgress != null && action !is GitAction.Stage && action !is GitAction.Unstage) {
            return "A Git operation is in progress (${snapshot.operationInProgress}). Complete it in the terminal first."
        }
        if (snapshot.files.any { it.isConflict } && action !is GitAction.Stage && action !is GitAction.Unstage) {
            return "Resolve the conflicted files before this action."
        }
        val cleanRequired = action is GitAction.SwitchBranch || action is GitAction.Pull || action is GitAction.MergeBranch ||
            action is GitAction.CreateBranch && action.checkout
        if (cleanRequired && snapshot.files.isNotEmpty()) return "This action requires a clean worktree and index."
        return when (action) {
            is GitAction.Stage -> paths(snapshot, action.paths, staged = false)
            is GitAction.Unstage -> paths(snapshot, action.paths, staged = true)
            is GitAction.Discard -> if (snapshot.head == null) "Create the first commit before discarding changes."
                else paths(snapshot, action.paths, staged = false)
            is GitAction.Commit -> when {
                action.message.isBlank() || '\u0000' in action.message -> "Enter a non-empty commit message."
                action.message.length > 65_536 -> "The commit message exceeds the safe size limit."
                snapshot.files.none { it.hasStaged } -> "Stage changes before committing."
                else -> null
            }
            is GitAction.Fetch -> remote(snapshot, action.remote)
            is GitAction.Pull -> remote(snapshot, action.remote) ?: branchName(context, action.branch) ?:
                if (snapshot.branchName == null || snapshot.head == null) "Pull requires a local branch with a commit." else null
            is GitAction.Push -> remote(snapshot, action.remote) ?: branchName(context, action.branch) ?:
                if (snapshot.branchName == null || snapshot.head == null) "Push requires a local branch with a commit." else null
            is GitAction.CreateBranch -> branchName(context, action.name) ?: when {
                snapshot.branches.any { !it.isRemote && it.name == action.name } -> "That local branch already exists."
                snapshot.head == null -> "Create the first commit before creating another branch."
                action.startPoint?.let { target(snapshot, it) == null } == true -> "Select an existing, unambiguous branch as the start point."
                else -> null
            }
            is GitAction.SwitchBranch -> local(snapshot, action.name) ?: branchName(context, action.name) ?:
                if (snapshot.branches.any { !it.isRemote && it.name == action.name && it.checkedOutPath != null && !it.isCurrent })
                    "That branch is checked out in another worktree." else null
            is GitAction.RenameBranch -> local(snapshot, action.oldName) ?: branchName(context, action.newName) ?: when {
                protected(action.oldName) -> "Protected branches cannot be renamed."
                snapshot.branches.any { !it.isRemote && it.name == action.newName } -> "That local branch already exists."
                snapshot.branches.any { !it.isRemote && it.name == action.oldName && it.checkedOutPath != null && !it.isCurrent } -> "That branch is checked out in another worktree."
                else -> null
            }
            is GitAction.DeleteBranch -> local(snapshot, action.name) ?: when {
                protected(action.name) -> "Protected branches cannot be deleted."
                snapshot.branches.any { !it.isRemote && it.name == action.name && (it.isCurrent || it.checkedOutPath != null) } -> "Checked-out branches cannot be deleted."
                !action.force && snapshot.branches.any { !it.isRemote && it.name == action.name && !it.isMerged } -> "The branch is not merged into the current HEAD."
                else -> null
            }
            is GitAction.DeleteTag -> if (action.name.startsWith('-') || action.name !in snapshot.tags) "Select an existing local tag." else null
            is GitAction.RemoveWorktree -> snapshot.worktrees.singleOrNull { it.path == action.path }.let { worktree ->
                when {
                    worktree == null -> "Select an existing worktree."
                    worktree.isMain -> "The main worktree cannot be removed."
                    worktree.path == snapshot.rootPath -> "The worktree this view is on cannot be removed here."
                    worktree.locked -> "The worktree is locked. Unlock it in the terminal first."
                    worktree.prunable -> "The worktree folder is gone. Prune stale worktrees instead."
                    else -> null
                }
            }
            GitAction.PruneWorktrees -> if (snapshot.worktrees.none { it.prunable }) "No stale worktrees to prune." else null
            is GitAction.MergeBranch -> when {
                snapshot.head == null -> "Create the first commit before merging."
                target(snapshot, action.name) == null -> "Select an existing, unambiguous branch to merge."
                else -> null
            }
        }
    }

    fun target(snapshot: GitRepositorySnapshot, name: String): String? {
        val branches = snapshot.branches.filter {
            val qualified = (if (it.isRemote) "refs/remotes/" else "refs/heads/") + it.name
            if (name.startsWith("refs/")) name == qualified else it.name == name
        }
        if (branches.size != 1) return null
        val selected = branches.single()
        return (if (selected.isRemote) "refs/remotes/" else "refs/heads/") + selected.name
    }

    private fun paths(snapshot: GitRepositorySnapshot, paths: List<String>, staged: Boolean): String? {
        if (paths.isEmpty() || paths.size > 10_000) return "Select a bounded set of changed paths."
        val selected = paths.map { path -> snapshot.files.singleOrNull { it.path == path } }
        if (selected.any { it == null }) return "Select only paths in the reviewed changes."
        if (staged && selected.any { it?.hasStaged != true }) return "Select only staged paths to unstage."
        return null
    }
    private fun remote(snapshot: GitRepositorySnapshot, name: String): String? =
        if (name in snapshot.remotes) null else "Select a configured remote by name."
    private fun local(snapshot: GitRepositorySnapshot, name: String): String? =
        if (snapshot.branches.any { !it.isRemote && it.name == name }) null else "Select an existing local branch."
    private fun branchName(context: GitRepositoryDiscovery, name: String): String? {
        if (name.isEmpty() || name.startsWith('-') || '\u0000' in name || name.length > 1_024) return "Enter a valid branch name."
        val result = runner.run(context.root, listOf("check-ref-format", "refs/heads/$name"))
        return if (result.exitCode == 0 && !result.uncertain) null else "Enter a valid branch name."
    }
    private fun protected(name: String): Boolean {
        val value = name.lowercase()
        return value in setOf("main", "master", "trunk", "develop", "release") || value.startsWith("release/") || value.startsWith("release-")
    }
}
