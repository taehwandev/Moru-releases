package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitRepositoryState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitIndexIntegrationTest {
    @Test fun missingAndNonRepositoryFoldersAreUnavailable(): Unit = GitTestRepository().use { fixture ->
        assertIs<GitRepositoryState.Unavailable>(fixture.adapter.snapshot(fixture.base.resolve("missing").toString()))
        assertIs<GitRepositoryState.Unavailable>(fixture.adapter.snapshot(fixture.base.toString()))
    }

    @Test fun stagesLiteralFilenamesAndCreatesTheFirstCommit() = GitTestRepository().use { fixture ->
        val names = listOf("space file.txt", "line\nfile.txt", ":(glob)*.txt", "--help", "a[1].txt", "\$(touch owned)")
        names.forEach { fixture.write(it, "literal") }
        val initial = fixture.snapshot()
        assertEquals(null, initial.head)
        assertEquals(names.toSet(), initial.files.map { it.path }.toSet())
        fixture.success(GitAction.Stage(listOf(":(glob)*.txt")))
        assertEquals(listOf(":(glob)*.txt"), fixture.snapshot().files.filter { it.hasStaged }.map { it.path })
        fixture.success(GitAction.Unstage(listOf(":(glob)*.txt")))
        assertEquals("literal", Files.readString(fixture.root.resolve(":(glob)*.txt")))
        fixture.success(GitAction.Stage(names))
        fixture.success(GitAction.Commit("Initial literal files"))
        assertTrue(fixture.snapshot().files.isEmpty())
        assertFalse(Files.exists(fixture.root.resolve("owned")))
    }

    @Test fun stageAndUnstageKeepBothRenameSidesAndWorktreeContent() = GitTestRepository().use { fixture ->
        val old = "old\n[1].txt"
        val new = "new *.txt"
        val original = "original-content\nsecond-line\nthird-line"
        val updated = "$original\nupdated line"
        fixture.commit(original, old)
        fixture.git("mv", "--", old, new)
        fixture.write(new, updated)
        assertEquals(old, fixture.snapshot().files.single().originalPath)
        fixture.success(GitAction.Stage(listOf(new)))
        assertEquals(updated, fixture.git("show", ":$new"))
        fixture.success(GitAction.Unstage(listOf(new)))
        assertTrue(fixture.snapshot().files.none { it.hasStaged })
        assertEquals(original, fixture.git("show", ":$old"))
        assertEquals(updated, Files.readString(fixture.root.resolve(new)))
    }

    @Test fun discardPutsCommittedFilesBackAndDeletesNewOnesOnlyForTheChosenPaths() = GitTestRepository().use { fixture ->
        fixture.commit("committed", "tracked.txt")
        fixture.write("tracked.txt", "staged edit")
        fixture.success(GitAction.Stage(listOf("tracked.txt")))
        fixture.write("tracked.txt", "working edit")
        fixture.write("new file.txt", "brand new")
        fixture.success(GitAction.Stage(listOf("new file.txt")))
        fixture.write("other.txt", "keep me")
        fixture.success(GitAction.Discard(listOf("tracked.txt", "new file.txt")))
        assertEquals("committed", Files.readString(fixture.root.resolve("tracked.txt")))
        assertFalse(Files.exists(fixture.root.resolve("new file.txt")))
        assertEquals(listOf("other.txt"), fixture.snapshot().files.map { it.path })
    }

    @Test fun commitUsesOnlyTheExistingIndexIncludingPartialStaging() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.write("tracked.txt", "staged version")
        fixture.success(GitAction.Stage(listOf("tracked.txt")))
        fixture.write("tracked.txt", "unstaged version")
        fixture.write("unrelated.txt", "keep untracked")
        val file = fixture.snapshot().files.single { it.path == "tracked.txt" }
        assertTrue(file.hasStaged && file.hasUnstaged)
        fixture.success(GitAction.Commit("Only the reviewed index"))
        assertEquals("staged version", fixture.git("show", "HEAD:tracked.txt"))
        assertEquals("unstaged version", Files.readString(fixture.root.resolve("tracked.txt")))
        assertTrue(fixture.snapshot().files.single { it.path == "unrelated.txt" }.isUntracked)
    }

    @Test fun unbornUnstageKeepsAFileWhoseWorktreeDiffersFromItsIndex() = GitTestRepository().use { fixture ->
        fixture.write("first.txt", "index")
        fixture.git("add", "--", "first.txt")
        fixture.write("first.txt", "worktree")
        fixture.success(GitAction.Unstage(listOf("first.txt")))
        assertEquals("worktree", Files.readString(fixture.root.resolve("first.txt")))
        assertTrue(fixture.snapshot().files.single().isUntracked)
    }
}
