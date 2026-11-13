package app.thdev.moru.core.model.git

/** Repository-relative literal paths; a renamed entry retains both sides. */
data class GitWorkingFile(
    val path: String,
    val indexStatus: Char,
    val worktreeStatus: Char,
    val originalPath: String? = null,
) {
    val isUntracked: Boolean get() = indexStatus == '?' && worktreeStatus == '?'
    val hasStaged: Boolean get() = !isUntracked && indexStatus != ' '
    val hasUnstaged: Boolean get() = isUntracked || worktreeStatus != ' '
    val isConflict: Boolean get() = "$indexStatus$worktreeStatus" in setOf("DD", "AU", "UD", "UA", "DU", "AA", "UU")
}
