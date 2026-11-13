package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class GitFreshnessIntegrationTest {
    @Test fun sameStatusSizeAndTimestampStillInvalidatesApprovalWhenContentChanges() = GitTestRepository().use { fixture ->
        fixture.commit()
        val path = fixture.write("tracked.txt", "version1")
        val timestamp = Files.getLastModifiedTime(path)
        val approved = fixture.snapshot()
        fixture.write("tracked.txt", "version2")
        Files.setLastModifiedTime(path, timestamp)
        val fresh = fixture.snapshot()
        assertEquals(approved.files, fresh.files)
        assertNotEquals(approved.revision, fresh.revision)
        assertIs<GitActionResult.Rejected>(fixture.adapter.execute(fixture.root.toString(), approved.revision, GitAction.Stage(listOf("tracked.txt"))))
        assertEquals("initial", fixture.git("show", ":tracked.txt"))
    }

    @Test fun aPreviouslyCleanFileBecomingDirtyInvalidatesAnApprovedAction() = GitTestRepository().use { fixture ->
        fixture.commit()
        val approved = fixture.snapshot()
        fixture.write("tracked.txt", "changed after review")
        assertIs<GitActionResult.Rejected>(fixture.adapter.execute(fixture.root.toString(), approved.revision, GitAction.CreateBranch("topic")))
        assertEquals("main", fixture.snapshot().branchName)
    }

    @Test fun indexBlobChangesInvalidateApprovalWithoutChangingStatusLetters(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.write("tracked.txt", "index1")
        fixture.git("add", "tracked.txt")
        fixture.write("tracked.txt", "working1")
        val approved = fixture.snapshot()
        fixture.write("tracked.txt", "index2")
        fixture.git("add", "tracked.txt")
        fixture.write("tracked.txt", "working2")
        assertEquals(approved.files, fixture.snapshot().files)
        assertIs<GitActionResult.Rejected>(fixture.adapter.execute(fixture.root.toString(), approved.revision, GitAction.Commit("Stale index")))
    }

    @Test fun refsRemoteConfigurationAndWorktreeOccupancyInvalidateRevision() = GitTestRepository().use { fixture ->
        fixture.commit()
        var previous = fixture.snapshot().revision
        fixture.git("branch", "topic")
        assertNotEquals(previous, fixture.snapshot().revision)
        fixture.git("remote", "add", "origin", fixture.bare().toString())
        previous = fixture.snapshot().revision
        fixture.git("config", "remote.origin.url", "https://example.invalid/changed-repository")
        assertNotEquals(previous, fixture.snapshot().revision)
        previous = fixture.snapshot().revision
        fixture.git("worktree", "add", fixture.base.resolve("linked").toString(), "topic")
        assertNotEquals(previous, fixture.snapshot().revision)
        assertEquals(fixture.base.resolve("linked").toRealPath().toString(), fixture.snapshot().branches.single { it.name == "topic" }.checkedOutPath)
    }
}
