package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.model.git.GitWorktreeInfo
import app.thdev.moru.feature.git.components.branches.GitRefMenuItem

/**
 * What the sidebar's right-click menus can do. Every change goes through [onEvent] as a proposal, so it still asks for
 * the usual approval; the editors only open the name forms, and [copy] puts text on the clipboard.
 */
internal class GitRefMenus(
    private val state: GitManagementUiState,
    private val onEvent: (GitManagementEvent) -> Unit,
    private val onCreateFrom: (GitBranchInfo) -> Unit,
    private val onRename: (GitBranchInfo) -> Unit,
    private val copy: (String) -> Unit,
) {
    private val snapshot: GitRepositorySnapshot? get() = state.snapshot
    private val available: Boolean get() = state.interactive && snapshot?.operationInProgress == null
    private val clean: Boolean get() = snapshot?.files?.isEmpty() == true

    private fun propose(action: GitAction) = onEvent(GitManagementEvent.Propose(action))

    fun branch(branch: GitBranchInfo): List<GitRefMenuItem> {
        val snapshot = snapshot ?: return emptyList()
        val current = snapshot.branchName
        val occupied = branch.checkedOutPath != null && branch.checkedOutPath != snapshot.rootPath
        val free = !branch.isCurrent && branch.checkedOutPath == null
        val name = branch.name
        return buildList {
            if (!branch.isRemote) {
                add(GitRefMenuItem("Switch to $name", "git-switch-branch",
                    available && clean && !branch.isCurrent && !occupied) { propose(GitAction.SwitchBranch(name)) })
            }
            if (current != null && !branch.isCurrent) {
                add(GitRefMenuItem("Merge $name into $current", "git-merge-branch", available && clean) {
                    propose(GitAction.MergeBranch(branch.actionRef))
                })
            }
            add(GitRefMenuItem(if (branch.isRemote) "Create local branch…" else "New branch from $name…", "git-create-open",
                available) { onCreateFrom(branch) })
            if (!branch.isRemote) {
                add(GitRefMenuItem("Rename…", "git-rename-open", available && !occupied) { onRename(branch) })
                if (branch.isMerged || branch.isCurrent) {
                    add(GitRefMenuItem("Delete $name", "git-delete-branch", available && free) {
                        propose(GitAction.DeleteBranch(name))
                    })
                } else {
                    add(GitRefMenuItem("Delete $name (not merged)…", "git-force-delete-branch", available && free) {
                        propose(GitAction.DeleteBranch(name, force = true))
                    })
                }
            }
            add(GitRefMenuItem("Copy branch name", "git-menu-copy") { copy(name) })
        }
    }

    fun worktree(worktree: GitWorktreeInfo): List<GitRefMenuItem> {
        val snapshot = snapshot ?: return emptyList()
        val removable = !worktree.isMain && worktree.path != snapshot.rootPath && !worktree.locked && !worktree.prunable
        return buildList {
            if (worktree.prunable) {
                add(GitRefMenuItem("Prune missing worktrees", "git-prune-worktrees", available) { propose(GitAction.PruneWorktrees) })
            } else {
                add(GitRefMenuItem("Remove worktree", "git-remove-worktree", available && removable) {
                    propose(GitAction.RemoveWorktree(worktree.path))
                })
            }
            add(GitRefMenuItem("Copy folder path", "git-menu-copy") { copy(worktree.path) })
        }
    }

    fun tag(name: String): List<GitRefMenuItem> = listOf(
        GitRefMenuItem("Delete tag $name", "git-delete-tag", available) { propose(GitAction.DeleteTag(name)) },
        GitRefMenuItem("Copy tag name", "git-menu-copy") { copy(name) },
    )
}
