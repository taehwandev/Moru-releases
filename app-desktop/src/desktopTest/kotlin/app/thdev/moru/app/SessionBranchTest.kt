package app.thdev.moru.app

import app.thdev.moru.core.model.git.CodeWorktree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionBranchTest {
    private val main = CodeWorktree("/repo", "main", isMain = true)
    private val featureA = CodeWorktree("/repo/.claude/worktrees/a", "feature-a", isMain = false)
    private val featureB = CodeWorktree("/repo/.claude/worktrees/b", "feature-b", isMain = false)
    private val worktrees = listOf(main, featureA, featureB)

    @Test
    fun eachTerminalShowsTheWorktreeItWorksIn() {
        assertEquals("feature-a", sessionBranch(worktrees, listOf(featureA.path)))
        assertEquals("feature-b", sessionBranch(worktrees, listOf(featureB.path)))
    }

    @Test
    fun aSideWorktreeWinsOverTheMainCheckout() {
        // An agent stays in the main checkout while its commands run in its worktree.
        assertEquals("feature-a", sessionBranch(worktrees, listOf(main.path, featureA.path)))
    }

    @Test
    fun withoutAKnownCheckoutTheMainBranchShows() {
        assertEquals("main", sessionBranch(worktrees, emptyList()))
        assertEquals("main", sessionBranch(worktrees, listOf("/elsewhere")))
        assertEquals("main", sessionBranch(worktrees, listOf(main.path)))
    }

    @Test
    fun outsideGitNothingShows() {
        assertNull(sessionBranch(emptyList(), listOf("/tmp")))
    }

    @Test
    fun aDetachedWorktreeShowsItsFolderName() {
        val detached = CodeWorktree("/repo/.claude/worktrees/c", null, isMain = false)
        assertEquals("c", sessionBranch(worktrees + detached, listOf(detached.path)))
    }
}

class SessionBranchesTest {
    private val main = CodeWorktree("/repo", "main", isMain = true)
    private val featureA = CodeWorktree("/repo/.worktrees/a", "feature-a", isMain = false)
    private val featureB = CodeWorktree("/repo/.worktrees/b", "feature-b", isMain = false)
    private val worktrees = listOf(main, featureA, featureB)

    @Test
    fun aSeenWorktreeStaysAfterItsCommandsEnd() {
        val branches = SessionBranches()

        assertEquals("feature-a", branches.branch("s1", worktrees, listOf(main.path, featureA.path)))
        assertEquals("feature-a", branches.branch("s1", worktrees, listOf(main.path)))
        assertEquals("main", branches.branch("s2", worktrees, listOf(main.path)))
    }

    @Test
    fun aDifferentWorktreeReplacesTheSeenOne() {
        val branches = SessionBranches()
        branches.branch("s1", worktrees, listOf(featureA.path))

        assertEquals("feature-b", branches.branch("s1", worktrees, listOf(featureB.path)))
    }

    @Test
    fun aRemovedWorktreeFallsBackToMain() {
        val branches = SessionBranches()
        branches.branch("s1", worktrees, listOf(featureA.path))

        assertEquals("main", branches.branch("s1", listOf(main, featureB), listOf(main.path)))
    }
}
