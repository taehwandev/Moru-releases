package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class GitBranchIntegrationTest {
    @Test fun createsSwitchesRenamesAndDeletesMergedLocalBranches() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.success(GitAction.CreateBranch("feature"))
        assertEquals("feature", fixture.snapshot().branchName)
        fixture.success(GitAction.SwitchBranch("main"))
        fixture.success(GitAction.CreateBranch("topic", checkout = false))
        fixture.success(GitAction.RenameBranch("topic", "renamed"))
        fixture.success(GitAction.DeleteBranch("renamed"))
        assertFalse(fixture.snapshot().branches.any { it.name == "renamed" })
    }

    @Test fun dirtyWorktreeRejectsSwitchMergeAndPullWithoutTouchingIt() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("branch", "topic")
        fixture.git("remote", "add", "origin", fixture.bare().toString())
        fixture.write("untracked.txt", "keep me")
        listOf(GitAction.SwitchBranch("topic"), GitAction.MergeBranch("topic"), GitAction.Pull("origin", "main"),
            GitAction.CreateBranch("checkout-topic")).forEach { assertIs<GitActionResult.Rejected>(fixture.execute(it)) }
        assertEquals("main", fixture.snapshot().branchName)
    }

    @Test fun protectedAndCurrentBranchesCannotBeDeletedOrProtectedBranchesRenamed(): Unit = GitTestRepository().use { fixture ->
        fixture.commit()
        val names = listOf("main", "master", "trunk", "develop", "release", "release/stable", "release-candidate")
        names.forEach {
            if (it != "main") fixture.git("branch", it)
            assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.DeleteBranch(it)))
            assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.RenameBranch(it, "renamed")))
            if (it != "main") fixture.git("branch", "--delete", it)
        }
        fixture.success(GitAction.CreateBranch("feature"))
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.DeleteBranch("feature")))
    }

    @Test fun checkedOutBranchesInAnotherWorktreeAreProtected() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("worktree", "add", "-b", "linked", fixture.base.resolve("linked").toString())
        listOf(GitAction.DeleteBranch("linked"), GitAction.RenameBranch("linked", "other"), GitAction.SwitchBranch("linked"))
            .forEach { assertIs<GitActionResult.Rejected>(fixture.execute(it)) }
    }

    @Test fun unmergedBranchIsRetainedUntilFastForwardMergeSucceeds() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.success(GitAction.CreateBranch("feature"))
        fixture.commit("feature content", "feature.txt")
        fixture.success(GitAction.SwitchBranch("main"))
        assertIs<GitActionResult.Rejected>(fixture.execute(GitAction.DeleteBranch("feature")))
        fixture.success(GitAction.MergeBranch("feature"))
        assertEquals("feature content", fixture.git("show", "HEAD:feature.txt"))
        fixture.success(GitAction.DeleteBranch("feature"))
    }

    @Test fun divergentMergeDoesNotCreateAMergeCommitOrOperation() = GitTestRepository().use { fixture ->
        fixture.commit()
        fixture.git("branch", "feature")
        fixture.commit("main change", "main.txt")
        val head = fixture.snapshot().head
        fixture.git("switch", "feature")
        fixture.commit("feature change", "feature.txt")
        fixture.git("switch", "main")
        assertIs<GitActionResult.Failed>(fixture.execute(GitAction.MergeBranch("feature")))
        assertEquals(head, fixture.snapshot().head)
        assertEquals(null, fixture.snapshot().operationInProgress)
    }
}
