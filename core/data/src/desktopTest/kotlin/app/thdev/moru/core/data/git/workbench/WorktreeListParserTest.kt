package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.CodeWorktree
import kotlin.test.Test
import kotlin.test.assertEquals

class WorktreeListParserTest {
    @Test
    fun readsMainFirstWithBranchesAndDetachedCheckouts() {
        val output = """
            worktree /repo
            HEAD 1111111111111111111111111111111111111111
            branch refs/heads/main

            worktree /repo/.claude/worktrees/a
            HEAD 2222222222222222222222222222222222222222
            branch refs/heads/feature/a

            worktree /repo/.claude/worktrees/b
            HEAD 3333333333333333333333333333333333333333
            detached

        """.trimIndent()

        assertEquals(
            listOf(
                CodeWorktree("/repo", "main", isMain = true),
                CodeWorktree("/repo/.claude/worktrees/a", "feature/a", isMain = false),
                CodeWorktree("/repo/.claude/worktrees/b", null, isMain = false),
            ),
            parseWorktreeList(output),
        )
    }
}
