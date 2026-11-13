package app.thdev.moru.feature.git

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import app.thdev.moru.core.designsystem.theme.MoruTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GitNetworkInteractionTest {
    @Test
    fun switchingBranchesUsesTheNewUpstreamInsteadOfThePreviousPushTarget() {
        val original = GitManagementFixtures.snapshot
        val state = GitManagementUiState().afterRead(GitRepositoryState.Loaded(original)).copy(remoteBranch = "published/old-topic")
        val next = original.copy(branchName = "main", upstream = "origin/main", upstreamBranch = "main")
        assertEquals("main", state.afterRead(GitRepositoryState.Loaded(next)).remoteBranch)
    }

    @Test
    fun refreshingPreservesAnAvailableSelectedTargetAndDropsRemovedRefs() {
        val original = GitManagementFixtures.snapshot.copy(branches = GitManagementFixtures.snapshot.branches +
            GitBranchInfo("origin/published/topic", isRemote = true))
        val state = GitManagementUiState().afterRead(GitRepositoryState.Loaded(original)).copy(remoteBranch = "published/topic")
        assertEquals("published/topic", state.afterRead(GitRepositoryState.Loaded(original.copy(revision = "fresh-r2"))).remoteBranch)
        assertEquals("topic", state.afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot)).remoteBranch)
    }

    @Test
    fun fetchRunsOnceFromTheBranchesRemoteWithoutASecondConfirmation() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        runOnIdle { assertEquals(0, fake.calls.size) }
        onNodeWithTag("git-fetch").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-confirm").assertDoesNotExist()
        runOnIdle { assertEquals(GitCall(GitManagementFixtures.Folder, "reviewed-r1", GitAction.Fetch("origin")), fake.calls.single()) }
    }

    @Test
    fun pushGoesToTheBranchesTrackingBranchAndShowsAuthenticationFailureInline() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply { result = GitActionResult.Failed("Authentication unavailable") }
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-push").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-confirm").assertDoesNotExist()
        onNodeWithTag("git-feedback").assertTextContains("Failed: Authentication unavailable")
        runOnIdle {
            val push = fake.calls.single().action as GitAction.Push
            assertEquals("origin" to "topic", push.remote to push.branch)
            assertEquals(2, fake.reads.size)
        }
    }

    @Test
    fun pullRequiresACleanFolderAndPullsTheBranchesTrackingBranch() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-pull").assertIsNotEnabled()
        fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(files = emptyList(), revision = "clean-r2"))
        onNodeWithTag("git-refresh").performClick()
        awaitGitIdle()
        onNodeWithTag("git-pull").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-confirm").assertDoesNotExist()
        runOnIdle { assertEquals(GitCall(GitManagementFixtures.Folder, "clean-r2", GitAction.Pull("origin", "topic")), fake.calls.single()) }
    }

    @Test
    fun pickingAnotherBranchAsksToSwitchSoCommitsGoThere() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply {
            readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(files = emptyList(), revision = "clean-r2"))
        }
        showGit(fake)
        awaitGitTag()
        onNodeWithContentDescription("Branch: topic").assertExists()
        onNodeWithTag("git-repository-name").assertExists()
        selectTarget("git-branch-picker", "reviewed")
        awaitGitTag("git-confirm")
        runOnIdle { assertTrue(fake.calls.isEmpty(), "Switching waits for the confirmation") }
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { assertEquals(GitAction.SwitchBranch("reviewed"), fake.calls.single().action) }
    }

    @Test
    fun aWorktreesBranchOpensThatWorktreeAndChangesHideSwitchableBranches() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        val opened = mutableListOf<String>()
        setContent {
            MoruTheme {
                GitManagementScreen(GitManagementFixtures.Folder, fake, history = NoHistory, onOpenWorktree = { opened += it })
            }
        }
        awaitGitTag()
        // With uncommitted changes nothing can be switched to, but the other worktree can still be opened.
        onNodeWithTag("git-branch-picker").performClick()
        val options = hasAnyAncestor(hasTestTag("git-branch-picker-options"))
        onNode(hasText("topic") and options).assertExists()
        assertTrue(onAllNodes(hasText("reviewed") and options).fetchSemanticsNodes().isEmpty())
        val menu = java.io.File("build/ui-captures/git-branch-picker-open.png")
        menu.parentFile.mkdirs()
        javax.imageio.ImageIO.write(onNode(hasTestTag("git-branch-picker-options")).captureToImage().toAwtImage(), "png", menu)
        // The search field narrows the list.
        onNode(hasSetTextAction() and options).performTextInput("bus")
        assertTrue(onAllNodes(hasText("topic") and options).fetchSemanticsNodes().isEmpty())
        onNode(hasText("busy") and options).performClick()
        runOnIdle {
            assertEquals(listOf("/work/another worktree"), opened)
            assertTrue(fake.calls.isEmpty(), "Opening a worktree runs no Git action")
        }
    }

    private fun ComposeUiTest.selectTarget(tag: String, value: String) {
        onNodeWithTag(tag).performClick()
        // The same ref also names a row in the branch list, so pick it inside the open menu.
        onNode(hasText(value) and hasAnyAncestor(hasTestTag("$tag-options"))).performClick()
    }
}
