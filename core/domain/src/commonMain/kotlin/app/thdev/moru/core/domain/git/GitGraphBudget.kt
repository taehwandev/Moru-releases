package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.history.GitCommit

/** Optional host preview limits. The unbounded compatibility caller keeps its existing graph semantics. */
data class GitGraphBudget(
    val maxParentsPerCommit: Int,
    val maxTotalParents: Int,
    val maxActiveLanes: Int,
    val maxProjectedElements: Int,
) {
    init {
        require(maxParentsPerCommit > 0 && maxTotalParents > 0 && maxActiveLanes > 0 && maxProjectedElements > 0)
    }

    class Exceeded(limit: String) : IllegalArgumentException("Git history exceeds the preview graph limit ($limit).")

    /** Reject parent fanout before the lane walk or scalar graph projection allocates its result. */
    fun checkParents(commits: List<GitCommit>) {
        var remaining = maxTotalParents
        for (commit in commits) {
            if (commit.parents.size > maxParentsPerCommit) throw Exceeded("$maxParentsPerCommit parents per commit")
            if (commit.parents.size > remaining) throw Exceeded("$maxTotalParents total parents")
            remaining -= commit.parents.size
        }
    }
}
