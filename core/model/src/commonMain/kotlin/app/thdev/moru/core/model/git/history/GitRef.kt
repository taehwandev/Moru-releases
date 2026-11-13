package app.thdev.moru.core.model.git.history

/**
 * A branch, tag or HEAD pointing at a commit. [isCurrent] marks what is checked out. A [Kind.Worktree] ref is another
 * checkout of the same repository, named after its folder, with [pendingChanges] files changed there and not committed.
 */
data class GitRef(
    val name: String,
    val kind: Kind,
    val isCurrent: Boolean = false,
    val pendingChanges: Int = 0,
) {
    enum class Kind {
        /** HEAD itself, only seen when it is detached. */
        Head,
        LocalBranch,
        RemoteBranch,
        Tag,

        /** Where another worktree of the repository has its HEAD, branch or detached. */
        Worktree,
    }
}
