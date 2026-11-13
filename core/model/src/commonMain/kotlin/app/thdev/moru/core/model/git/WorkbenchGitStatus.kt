package app.thdev.moru.core.model.git

data class WorkbenchGitStatus(
    val repositoryDetected: Boolean,
    val branchName: String,
    val changes: List<WorkbenchGitChange>,
    val message: String? = null,
) {
    val changedCount: Int
        get() = changes.size

    val stagedCount: Int
        get() = changes.count { change -> change.indexStatus != null && change.indexStatus != '?' }

    val unstagedCount: Int
        get() = changes.count { change -> change.worktreeStatus != null && change.worktreeStatus != '?' }

    val untrackedCount: Int
        get() = changes.count { change -> change.isUntracked }

    companion object {
        val NotRepository = WorkbenchGitStatus(
            repositoryDetected = false,
            branchName = "no git",
            changes = emptyList(),
            message = "No git repository detected.",
        )
    }
}
