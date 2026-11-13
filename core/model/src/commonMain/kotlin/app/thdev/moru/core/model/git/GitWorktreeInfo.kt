package app.thdev.moru.core.model.git

/**
 * One checkout of the repository as `git worktree list` reports it: its [path], the [branch] it has out (null when
 * detached at [head]), whether it is the [isMain] checkout, and whether Git marks it [prunable] (its folder is gone)
 * or [locked].
 */
data class GitWorktreeInfo(
    val path: String,
    val branch: String?,
    val head: String?,
    val isMain: Boolean,
    val prunable: Boolean = false,
    val locked: Boolean = false,
)
