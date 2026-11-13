package app.thdev.moru.core.model.git.history

/**
 * The outcome of reading a repository's history. An empty repository is [Loaded] with no
 * commits, not an error. [Loaded.truncated] means older commits exist beyond the limit.
 */
sealed interface GitHistoryResult {
    data class Loaded(
        val commits: List<GitCommit>,
        val truncated: Boolean,
    ) : GitHistoryResult

    data class Unavailable(val message: String) : GitHistoryResult
}
