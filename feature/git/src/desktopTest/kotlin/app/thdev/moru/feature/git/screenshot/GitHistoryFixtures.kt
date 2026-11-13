package app.thdev.moru.feature.git.screenshot

import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.history.GitRef
/**
 * A deterministic history shaped like a real one: two merges, a feature branch, tags,
 * remote refs and a root commit. Newest first and topologically ordered, as
 * `git log --topo-order` returns it. Test-only; no real commits or addresses.
 */
internal object GitHistoryFixtures {
    const val NOW_EPOCH_SECONDS = 1_790_000_000L

    private const val HOUR = 3_600L
    private const val DAY = 86_400L

    private fun hash(prefix: String) = prefix.padEnd(40, '0')

    private fun commit(
        prefix: String,
        subject: String,
        ageSeconds: Long,
        vararg parents: String,
        author: String = "taehwandev",
        refs: List<GitRef> = emptyList(),
    ) = GitCommit(
        hash = hash(prefix),
        parents = parents.map { hash(it) },
        authorName = author,
        authorEmail = "dev@example.com",
        epochSeconds = NOW_EPOCH_SECONDS - ageSeconds,
        refs = refs,
        subject = subject,
    )

    val selectedHash: String = hash("5d2e9b4")

    fun commits(): List<GitCommit> = listOf(
        commit(
            "9f3a1c2", "Merge pull request #3 from feat/git-history", 3 * HOUR, "7c1b8d0", "5d2e9b4",
            refs = listOf(
                GitRef("main", GitRef.Kind.LocalBranch, isCurrent = true),
                GitRef("origin/main", GitRef.Kind.RemoteBranch),
            ),
        ),
        commit(
            "5d2e9b4", "feat(history): draw the commit graph", 5 * HOUR, "b41f0a6", author = "Claude",
            refs = listOf(
                GitRef("feat/git-history", GitRef.Kind.LocalBranch),
                GitRef("origin/feat/git-history", GitRef.Kind.RemoteBranch),
            ),
        ),
        commit(
            "b41f0a6", "feat(history): lay out commit graph lanes", 7 * HOUR, "e8a2c53", author = "Claude",
            refs = listOf(GitRef("agent-task", GitRef.Kind.Worktree, pendingChanges = 3)),
        ),
        commit("7c1b8d0", "docs(desktop): align the module lists", 9 * HOUR, "2a9f6e1"),
        commit("e8a2c53", "feat(history): parse git log output", 11 * HOUR, "2a9f6e1", author = "Claude"),
        commit("2a9f6e1", "chore(desktop): remove unreachable modules", 1 * DAY, "d03b7a9"),
        commit(
            "d03b7a9", "Merge pull request #2 from feat/rename-moru", 3 * DAY, "6e41c88", "a7f5d12",
            refs = listOf(GitRef("v0.2.0", GitRef.Kind.Tag)),
        ),
        commit("a7f5d12", "refactor: rename CrewKit to Moru", 4 * DAY, "f19c3b0"),
        commit("6e41c88", "feat(workbench): task-first Project and Task sidebar", 5 * DAY, "3b8d0e7"),
        commit("f19c3b0", "docs: rename AgentPlaybook to Moru Core", 6 * DAY, "3b8d0e7"),
        commit("3b8d0e7", "feat(terminal): render ANSI colors with the app palette", 12 * DAY, "c25a7f4"),
        commit(
            "c25a7f4", "fix(terminal): restore color capabilities in desktop PTYs", 20 * DAY, "81e6d09",
            refs = listOf(GitRef("v0.1.0", GitRef.Kind.Tag), GitRef("origin/release", GitRef.Kind.RemoteBranch)),
        ),
        commit("81e6d09", "feat: add the first native workbench shell", 40 * DAY, "0f7b3c5"),
        commit("0f7b3c5", "chore: bootstrap the KMP project", 41 * DAY),
    )

    /**
     * Fourteen branches that all start from one root, as a repository with many open branches reads under
     * `git log --all`: the graph is as wide as it gets, and every subject still has to be readable.
     */
    fun manyBranches(): List<GitCommit> {
        val tips = (1..14).map { index ->
            commit(
                "t%02d".format(index), "feat(area-$index): a branch tip whose subject must stay readable",
                index * HOUR, "p%02d".format(index),
            )
        }
        val bases = (1..14).map { index ->
            commit("p%02d".format(index), "chore(area-$index): earlier work on the same branch", 20 * HOUR + index * HOUR, "root")
        }
        return tips + bases + commit("root", "chore: initial commit", 30 * DAY)
    }
}
