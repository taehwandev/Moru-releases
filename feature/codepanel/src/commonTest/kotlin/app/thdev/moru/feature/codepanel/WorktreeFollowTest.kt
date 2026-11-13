package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.git.CodeWorktree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class WorktreeFollowTest {
    private val main = CodeWorktree("/repo", "main", isMain = true)
    private val a = CodeWorktree("/repo/.claude/worktrees/a", "feature-a", isMain = false)
    private val b = CodeWorktree("/repo/.claude/worktrees/b", "feature-b", isMain = false)
    private val all = listOf(main, a, b)

    @Test
    fun aTerminalOnlyInTheProjectFolderShowsMain() {
        assertEquals("/repo", WorktreeFollow().observe(listOf("/repo"), all).shown)
    }

    @Test
    fun aWorktreeSeenOnceStaysAfterItsCommandEnds() {
        val seen = WorktreeFollow().observe(listOf("/repo", a.path), all)
        val later = seen.observe(listOf("/repo"), all)

        assertEquals(a.path, seen.shown)
        assertEquals(a.path, later.shown)
        assertEquals(a.path, later.seen)
    }

    @Test
    fun aPickHoldsUntilTheTerminalMovesToAnotherWorktree() {
        val picked = WorktreeFollow().observe(listOf(a.path), all).pick(main.path)

        assertEquals(WorktreeFollow(main.path, a.path), picked)
        assertEquals(main.path, picked.observe(listOf(a.path), all).shown)
        assertEquals(b.path, picked.observe(listOf(b.path), all).shown)
    }

    @Test
    fun aRemovedWorktreeFallsBackToMain() {
        val seen = WorktreeFollow().observe(listOf(a.path), all)

        assertEquals(WorktreeFollow(main.path, null), seen.observe(emptyList(), listOf(main, b)))
    }

    @Test
    fun foldersOutsideTheRepositoryAreIgnored() {
        assertEquals(main.path, WorktreeFollow().observe(listOf("/elsewhere"), all).shown)
    }

    @Test
    fun aFailedWorktreeReadingKeepsTheChoice() {
        val seen = WorktreeFollow().observe(listOf(a.path), all)

        assertEquals(seen, seen.observe(emptyList(), emptyList()))
        assertSame(seen, seen.observe(emptyList(), emptyList()))
    }
}
