package app.thdev.moru.core.model.git.diff

/**
 * The outcome of reading one commit's changes. [Loaded.truncated] means the patch was cut at the
 * size limit, so the files shown are a prefix of the real change. An empty commit is [Loaded] with
 * no files, not an error.
 */
sealed interface CommitDiffResult {
    data class Loaded(
        val files: List<DiffFile>,
        val truncated: Boolean,
    ) : CommitDiffResult

    data class Unavailable(val message: String) : CommitDiffResult
}
