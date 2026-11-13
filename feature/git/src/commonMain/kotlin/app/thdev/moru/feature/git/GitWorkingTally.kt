package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitWorkingFile

/**
 * How many uncommitted files were changed, added or deleted, staged or not. A new file counts as added whether it is
 * untracked or already staged; a conflict counts as changed.
 */
internal data class GitWorkingTally(val modified: Int, val added: Int, val deleted: Int)

internal enum class GitWorkingKind { Modified, Added, Deleted }

internal fun GitWorkingFile.workingKind(): GitWorkingKind = when {
    isConflict -> GitWorkingKind.Modified
    isUntracked || indexStatus == 'A' -> GitWorkingKind.Added
    indexStatus == 'D' || worktreeStatus == 'D' -> GitWorkingKind.Deleted
    else -> GitWorkingKind.Modified
}

internal fun gitWorkingTally(files: List<GitWorkingFile>): GitWorkingTally {
    val kinds = files.map { it.workingKind() }
    return GitWorkingTally(
        modified = kinds.count { it == GitWorkingKind.Modified },
        added = kinds.count { it == GitWorkingKind.Added },
        deleted = kinds.count { it == GitWorkingKind.Deleted },
    )
}
