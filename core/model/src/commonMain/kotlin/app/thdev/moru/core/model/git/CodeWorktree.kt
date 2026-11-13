package app.thdev.moru.core.model.git

/** One checkout of the Project's repository: its root [path], the [branch] it has out (null when detached), and whether it is the main one. */
data class CodeWorktree(
    val path: String,
    val branch: String?,
    val isMain: Boolean,
) {
    /** What the picker shows: the branch, or the folder name when no branch is out. */
    val label: String get() = branch ?: path.substringAfterLast('/')
}
