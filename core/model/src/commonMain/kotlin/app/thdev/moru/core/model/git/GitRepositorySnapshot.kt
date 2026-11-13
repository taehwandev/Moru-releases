package app.thdev.moru.core.model.git

/** A complete read of the repository the user is about to change. */
data class GitRepositorySnapshot(
    val rootPath: String,
    val branchName: String?,
    val head: String?,
    val files: List<GitWorkingFile>,
    val branches: List<GitBranchInfo>,
    val remotes: List<String>,
    val revision: String,
    val upstream: String? = null,
    val upstreamRemote: String? = null,
    val upstreamBranch: String? = null,
    val ahead: Int? = null,
    val behind: Int? = null,
    val operationInProgress: String? = null,
    /** Local tag names, newest first. */
    val tags: List<String> = emptyList(),
    /** Every checkout of this repository, the main one first. */
    val worktrees: List<GitWorktreeInfo> = emptyList(),
)
