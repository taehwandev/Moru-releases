package app.thdev.moru.core.model.git

data class WorkbenchGitChange(
    val path: String,
    val indexStatus: Char?,
    val worktreeStatus: Char?,
    val originalPath: String? = null,
) {
    val isUntracked: Boolean
        get() = indexStatus == '?' && worktreeStatus == '?'

    val shortStatus: String
        get() = when {
            isUntracked -> "??"
            indexStatus != null && worktreeStatus != null -> "$indexStatus$worktreeStatus"
            indexStatus != null -> "$indexStatus "
            worktreeStatus != null -> " $worktreeStatus"
            else -> "  "
        }
}
