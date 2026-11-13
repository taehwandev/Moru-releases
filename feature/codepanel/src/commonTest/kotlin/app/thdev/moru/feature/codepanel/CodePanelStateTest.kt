package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.SavedCodePanel
import app.thdev.moru.core.model.git.CodeWorktree
import kotlin.test.Test
import kotlin.test.assertEquals

class CodePanelStateTest {
    private val worktrees = listOf(
        CodeWorktree("/repo", "main", isMain = true),
        CodeWorktree("/repo/wt", "feature", isMain = false),
    )

    @Test
    fun eachTerminalKeepsItsOwnWorktreeAcrossRestore() {
        val state = CodePanelState(emptyMap())
        state.observe("task", "term-a", listOf("/repo/wt"), worktrees)
        state.observe("task", "term-b", listOf("/repo"), worktrees)

        val restored = CodePanelState(state.savedPanels)

        assertEquals("/repo/wt", restored.shown("task", "term-a"))
        assertEquals("/repo", restored.shown("task", "term-b"))
    }

    @Test
    fun restoresASavedChoice() {
        val state = CodePanelState(mapOf("task" to SavedCodePanel(worktrees = mapOf("term-a" to "/repo/wt"))))

        assertEquals("/repo/wt", state.shown("task", "term-a"))
    }
}
