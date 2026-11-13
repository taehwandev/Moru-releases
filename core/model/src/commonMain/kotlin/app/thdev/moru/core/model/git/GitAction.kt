package app.thdev.moru.core.model.git

/**
 * Closed action set: there is no arbitrary command, force push, or remote deletion. Deleting an unmerged local branch
 * is its own approved action ([DeleteBranch.force]); a worktree is removed only while it has no changes.
 */
sealed interface GitAction {
    data class Stage(val paths: List<String>) : GitAction
    data class Unstage(val paths: List<String>) : GitAction

    /** Puts [paths] back as the last commit has them, index and working file; a file new since then is deleted. */
    data class Discard(val paths: List<String>) : GitAction
    data class Commit(val message: String) : GitAction
    data class Fetch(val remote: String) : GitAction
    data class Pull(val remote: String, val branch: String) : GitAction
    data class Push(val remote: String, val branch: String, val setUpstream: Boolean = true) : GitAction
    data class CreateBranch(val name: String, val startPoint: String? = null, val checkout: Boolean = true) : GitAction
    data class SwitchBranch(val name: String) : GitAction
    data class RenameBranch(val oldName: String, val newName: String) : GitAction
    data class DeleteBranch(val name: String, val force: Boolean = false) : GitAction
    data class DeleteTag(val name: String) : GitAction
    data class RemoveWorktree(val path: String) : GitAction
    data object PruneWorktrees : GitAction
    data class MergeBranch(val name: String) : GitAction
}
