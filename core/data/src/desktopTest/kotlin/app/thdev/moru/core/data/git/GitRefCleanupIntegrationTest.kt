package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitRefCleanupIntegrationTest {
    @Test fun listsAndDeletesLocalTags(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("tag", "v1.0.0")
        fixture.git("tag", "--annotate", "--message", "release", "v1.1.0")
        assertEquals(listOf("v1.0.0", "v1.1.0"), fixture.snapshot().tags)
        fixture.success(GitAction.DeleteTag("v1.0.0"))
        assertEquals(listOf("v1.1.0"), fixture.snapshot().tags)
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.DeleteTag("missing")))
    }

    @Test fun forcedDeleteRemovesAnUnmergedBranchOnlyWhenAsked(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("switch", "--create", "feature")
        fixture.commit("feature content", "feature.txt")
        fixture.git("switch", "main")
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.DeleteBranch("feature")))
        fixture.success(GitAction.DeleteBranch("feature", force = true))
        assertFalse(fixture.snapshot().branches.any { it.name == "feature" })
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.DeleteBranch("main", force = true)))
    }

    @Test fun listsWorktreesAndRemovesOnlyACleanLinkedOne(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("worktree", "add", "-b", "linked", fixture.base.resolve("linked").toString())
        fixture.git("worktree", "add", "--detach", fixture.base.resolve("detached").toString())
        val worktrees = fixture.snapshot().worktrees
        assertEquals(3, worktrees.size)
        assertTrue(worktrees.first().isMain)
        assertEquals("main", worktrees.first().branch)
        val linked = worktrees.single { it.branch == "linked" }
        val detached = worktrees.single { !it.isMain && it.branch == null }
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.RemoveWorktree(worktrees.first().path)))

        Files.writeString(fixture.base.resolve("linked/dirty.txt"), "keep me")
        assertIs<GitActionResult.Failed>(fixture.execute(GitAction.RemoveWorktree(linked.path)))
        assertTrue(Files.exists(fixture.base.resolve("linked/dirty.txt")))

        fixture.success(GitAction.RemoveWorktree(detached.path))
        assertFalse(fixture.snapshot().worktrees.any { it.path == detached.path })
        // The branch stays; only the checkout folder goes.
        assertTrue(fixture.snapshot().branches.any { it.name == "linked" })
    }

    @Test fun prunesAWorktreeWhoseFolderIsGone(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        val folder = fixture.base.resolve("gone")
        fixture.git("worktree", "add", "-b", "gone", folder.toString())
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.PruneWorktrees))
        Files.walk(folder).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        assertTrue(fixture.snapshot().worktrees.single { it.branch == "gone" }.prunable)
        fixture.success(GitAction.PruneWorktrees)
        assertEquals(1, fixture.snapshot().worktrees.size)
    }
}
