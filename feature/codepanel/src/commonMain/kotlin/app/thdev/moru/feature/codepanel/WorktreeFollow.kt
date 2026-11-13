package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.domain.code.WorktreeFollowState
import app.thdev.moru.core.model.git.CodeWorktree

/**
 * Which checkout a terminal's Code panel shows. [shown] is that checkout's root, and [seen] the last checkout other than
 * the main one the terminal's programs were found working in.
 *
 * The shell stays in the project folder while an agent works in a worktree, so the main checkout is always seen and
 * never replaces another one. A checkout once seen stays shown, because the commands that reveal it come and go. A
 * user's pick holds until the terminal is seen in a different checkout, and a checkout that is gone falls back to main.
 */
data class WorktreeFollow(
    val shown: String? = null,
    val seen: String? = null,
) {
    /** One reading of the checkout [roots] the terminal's programs work in, against the repository's [worktrees]. */
    fun observe(roots: List<String>, worktrees: List<CodeWorktree>): WorktreeFollow {
        val state = WorktreeFollowState(shown, seen)
        val next = state.observe(roots, worktrees.map { it.path }, worktrees.firstOrNull { it.isMain }?.path)
        return if (next === state) this else WorktreeFollow(next.shown, next.seen)
    }

    fun pick(root: String): WorktreeFollow = WorktreeFollowState(shown, seen).pick(root).let { WorktreeFollow(it.shown, it.seen) }
}
