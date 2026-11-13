package app.thdev.moru.app

import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.feature.codepanel.WorktreeFollow
import kotlin.concurrent.Volatile

/**
 * The branch a terminal shows in its header: the worktree its programs work in, the first side worktree of
 * [sessionRoots] winning over the main checkout; with none of them in a known checkout, the main checkout's branch; and
 * nothing when the folder is not in a git repository at all.
 */
internal fun sessionBranch(worktrees: List<CodeWorktree>, sessionRoots: List<String>): String? {
    val side = sessionRoots.firstNotNullOfOrNull { root -> worktrees.firstOrNull { it.path == root && !it.isMain } }
    val shown = side
        ?: worktrees.firstOrNull { worktree -> worktree.isMain && worktree.path in sessionRoots }
        ?: worktrees.firstOrNull { worktree -> worktree.isMain }
    return shown?.label
}

/**
 * Each terminal's header branch, remembered across readings by the Code panel's [WorktreeFollow] rule: a side worktree
 * once seen stays until another one is seen or it is removed, because the commands that reveal it come and go.
 */
internal class SessionBranches {
    // Readers of different panes may race; losing one reading only delays the header to the next one.
    @Volatile
    private var follows = emptyMap<String, WorktreeFollow>()

    fun branch(sessionId: String, worktrees: List<CodeWorktree>, sessionRoots: List<String>): String? {
        val follow = (follows[sessionId] ?: WorktreeFollow()).observe(sessionRoots, worktrees)
        follows = follows + (sessionId to follow)
        return sessionBranch(worktrees, listOfNotNull(follow.seen) + sessionRoots)
    }
}
