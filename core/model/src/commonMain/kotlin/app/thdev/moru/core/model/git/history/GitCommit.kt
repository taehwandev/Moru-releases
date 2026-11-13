package app.thdev.moru.core.model.git.history

/** One commit as `git log` reports it. [parents] are full hashes, first parent first. */
data class GitCommit(
    val hash: String,
    val parents: List<String>,
    val authorName: String,
    val authorEmail: String,
    val epochSeconds: Long,
    val refs: List<GitRef>,
    val subject: String,
) {
    val shortHash: String get() = hash.take(SHORT_HASH_LENGTH)

    val isMerge: Boolean get() = parents.size > 1

    private companion object {
        const val SHORT_HASH_LENGTH = 7
    }
}
