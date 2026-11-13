package app.thdev.moru.feature.codepanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.model.SavedCodePanel

import app.thdev.moru.core.model.git.CodeWorktree
/** The checkout each terminal of each Task shows in the Code tab. Saved by Task id. */
class CodePanelState internal constructor(saved: Map<String, SavedCodePanel>) {
    private var follows by mutableStateOf(saved.mapValues { (_, panel) ->
        panel.worktrees.mapValues { (_, root) -> WorktreeFollow(shown = root) }
    }.filterValues { terminals -> terminals.isNotEmpty() })

    internal val savedPanels: Map<String, SavedCodePanel>
        get() = follows.keys.associateWith { task ->
            SavedCodePanel(
                worktrees = follows[task].orEmpty().mapNotNull { (terminal, follow) -> follow.shown?.let { terminal to it } }.toMap(),
            )
        }

    internal fun shown(task: String, terminal: String): String? = follows[task]?.get(terminal)?.shown

    internal fun observe(task: String, terminal: String, roots: List<String>, worktrees: List<CodeWorktree>) =
        update(task, terminal) { follow -> follow.observe(roots, worktrees) }

    internal fun pick(task: String, terminal: String, root: String) = update(task, terminal) { follow -> follow.pick(root) }

    private fun update(task: String, terminal: String, change: (WorktreeFollow) -> WorktreeFollow) {
        val terminals = follows[task].orEmpty()
        val next = change(terminals[terminal] ?: WorktreeFollow())
        if (terminals[terminal] != next) follows = follows + (task to terminals + (terminal to next))
    }
}
