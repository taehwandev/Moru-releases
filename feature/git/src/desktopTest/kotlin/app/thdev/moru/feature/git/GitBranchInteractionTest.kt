package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitRepositoryState
import app.thdev.moru.core.model.git.GitWorktreeInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.openMenu(rowTag: String) {
    onNodeWithTag("git-branches").performScrollToNode(hasTestTag(rowTag))
    onNodeWithTag(rowTag).performMouseInput { rightClick() }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
class GitBranchInteractionTest {
    @Test
    fun switchingRequiresACleanFolderAndApprovalNamesTheTarget() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        openMenu("git-select-branch-reviewed")
        onNodeWithTag("git-switch-branch").assertIsNotEnabled()
        onNodeWithTag("git-menu-copy").performClick()
        fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(files = emptyList(), revision = "clean-r2"))
        onNodeWithTag("git-refresh").performClick()
        awaitGitIdle()
        captureGit("git-branches")
        openMenu("git-select-branch-reviewed")
        onNodeWithTag("git-switch-branch").performClick()
        onNodeWithText("Target local branch: reviewed").assertExists()
        onNodeWithTag("git-cancel").performClick()
        runOnIdle { assertEquals(0, fake.calls.size) }
        openMenu("git-select-branch-reviewed")
        onNodeWithTag("git-switch-branch").performClick()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { assertEquals(GitCall(GitManagementFixtures.Folder, "clean-r2", GitAction.SwitchBranch("reviewed")), fake.calls.single()) }
    }

    @Test
    fun localRenameAndMergedDeletionAreSeparateApprovedActions() = runDesktopComposeUiTest(width = 1100, height = 1100) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-rename-name").assertDoesNotExist()
        openMenu("git-select-branch-reviewed")
        onNodeWithTag("git-rename-open").performClick()
        onNodeWithTag("git-rename-name").performTextReplacement("renamed/topic")
        onNodeWithTag("git-rename-branch").performClick()
        onNodeWithText("Local branch: reviewed").assertExists()
        onNodeWithText("New name: renamed/topic").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        openMenu("git-select-branch-reviewed")
        onNodeWithTag("git-delete-branch").performClick()
        onNodeWithText("Local branch: reviewed").assertExists()
        onNodeWithTag("git-cancel").performClick()
        runOnIdle { assertEquals(1, fake.calls.size) }
        openMenu("git-select-branch-reviewed")
        onNodeWithTag("git-delete-branch").performClick()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle {
            assertEquals(listOf(GitAction.RenameBranch("reviewed", "renamed/topic"), GitAction.DeleteBranch("reviewed")), fake.calls.map { it.action })
        }
    }

    @Test
    fun unmergedBranchIsDeletedOnlyThroughItsOwnWarnedAction() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val snapshot = GitManagementFixtures.snapshot.copy(branches = GitManagementFixtures.snapshot.branches +
            GitBranchInfo("stale/task", isMerged = false))
        val fake = FakeGitRepository().apply { readState = GitRepositoryState.Loaded(snapshot) }
        showGit(fake)
        awaitGitTag()
        openMenu("git-select-branch-stale/task")
        onNodeWithTag("git-delete-branch").assertDoesNotExist()
        onNodeWithTag("git-force-delete-branch").performClick()
        onNodeWithText("Delete unmerged local branch").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { assertEquals(GitAction.DeleteBranch("stale/task", force = true), fake.calls.single().action) }
    }

    @Test
    fun occupiedBranchCannotSwitchOrRenameAndRemoteCanCreateANamedLocalBranch() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-branches").performScrollToNode(hasTestTag("git-worktree-busy"))
        onNodeWithTag("git-worktree-busy").assertExists()
        openMenu("git-select-branch-busy")
        onNodeWithTag("git-switch-branch").assertIsNotEnabled()
        onNodeWithTag("git-rename-open").assertIsNotEnabled()
        onNodeWithTag("git-force-delete-branch").assertIsNotEnabled()
        onNodeWithTag("git-menu-copy").performClick()
        onNodeWithTag("git-create-name").assertDoesNotExist()
        openMenu("git-select-branch-origin/topic")
        onNodeWithTag("git-create-open").performClick()
        onNodeWithTag("git-create-name").assertTextContains("topic")
        onNodeWithTag("git-create-name").performTextReplacement("local/topic")
        onNodeWithTag("git-create-from-selected").performClick()
        onNodeWithText("New local branch: local/topic").assertExists()
        onNodeWithText("Create this branch without switching.").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { assertEquals(GitAction.CreateBranch("local/topic", "refs/remotes/origin/topic", checkout = false), fake.calls.single().action) }
    }

    @Test
    fun fastForwardMergeRequiresApprovalAndKeepsTheSourceRefUnambiguous() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply { readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(files = emptyList())) }
        showGit(fake)
        awaitGitTag()
        openMenu("git-select-branch-origin/topic")
        onNodeWithTag("git-merge-branch").performClick()
        onNodeWithText("Source branch: refs/remotes/origin/topic").assertExists()
        onNodeWithText("Target branch: topic").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { assertEquals(GitAction.MergeBranch("refs/remotes/origin/topic"), fake.calls.single().action) }
    }

    @Test
    fun tagsAndWorktreesAreListedAndCleanedUpFromTheirMenus() = runDesktopComposeUiTest(width = 1100, height = 900) {
        val linked = GitWorktreeInfo("/work/linked", "busy", "abc", isMain = false)
        val gone = GitWorktreeInfo("/work/gone", "gone", "def", isMain = false, prunable = true)
        val snapshot = GitManagementFixtures.snapshot.copy(tags = listOf("v1.0.0"), worktrees = listOf(
            GitWorktreeInfo(GitManagementFixtures.Folder, "topic", "012", isMain = true), linked, gone))
        val fake = FakeGitRepository().apply { readState = GitRepositoryState.Loaded(snapshot) }
        showGit(fake)
        awaitGitTag()
        openMenu("git-worktree-topic")
        onNodeWithTag("git-remove-worktree").assertIsNotEnabled()
        onNodeWithTag("git-menu-copy").performClick()
        openMenu("git-worktree-busy")
        onNodeWithTag("git-remove-worktree").performClick()
        onNodeWithText("Worktree folder: /work/linked").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        openMenu("git-worktree-gone")
        onNodeWithTag("git-prune-worktrees").performClick()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        openMenu("git-tag-v1.0.0")
        onNodeWithTag("git-delete-tag").performClick()
        onNodeWithText("Local tag: v1.0.0").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle {
            assertEquals(listOf(GitAction.RemoveWorktree("/work/linked"), GitAction.PruneWorktrees, GitAction.DeleteTag("v1.0.0")),
                fake.calls.map { it.action })
        }
    }

    @Test
    fun compactSidebarFoldsSectionsSearchesRefsAndKeepsFormsWithinItsWidth() = runDesktopComposeUiTest(width = 220, height = 480) {
        val longName = "feature/a-very-long-branch-name-for-terminal-navigation"
        val snapshot = GitManagementFixtures.snapshot.copy(files = emptyList(), branches = GitManagementFixtures.snapshot.branches +
            listOf(GitBranchInfo(longName, isMerged = true), GitBranchInfo("origin/HEAD", isRemote = true)))
        setContent {
            var state by remember { mutableStateOf(GitManagementUiState(
                repositoryState = GitRepositoryState.Loaded(snapshot), loading = false, selectedBranch = "local:topic")) }
            MoruTheme {
                GitRefsSidebar(state, { event -> state = when (event) {
                    is GitManagementEvent.SelectBranch -> state.copy(selectedBranch = event.key)
                    is GitManagementEvent.EditNewBranchName -> state.copy(newBranchName = event.name)
                    is GitManagementEvent.EditRenameBranchName -> state.copy(renameBranchName = event.name)
                    else -> state
                } }, Modifier.fillMaxSize())
            }
        }
        onNodeWithTag("git-create-name").assertDoesNotExist()
        onNodeWithTag("git-section-Local").performClick()
        onNodeWithTag("git-select-branch-topic").assertDoesNotExist()
        onNodeWithTag("git-section-Local").performClick()
        onNodeWithTag("git-branch-search").performTextInput("origin/HEAD")
        onNodeWithText("No matching refs").assertExists()
        onNodeWithTag("git-branch-search").performTextReplacement("VERY-LONG")
        onNodeWithTag("git-select-branch-$longName").assertContentDescriptionEquals("Local branch $longName").performClick()
        onNodeWithTag("git-select-branch-topic").assertDoesNotExist()
        onNodeWithTag("git-select-branch-origin/HEAD").assertDoesNotExist()
        val row = onNodeWithTag("git-select-branch-$longName").fetchSemanticsNode().boundsInRoot
        assertTrue(row.left >= 0 && row.right <= 220, "the row should fit the sidebar: $row")
        openMenu("git-select-branch-$longName")
        onNodeWithTag("git-rename-open").performClick()
        onNodeWithTag("git-rename-name").assertExists()
        onNodeWithTag("git-rename-cancel").performClick()
        onNodeWithTag("git-new-branch").performClick()
        onNodeWithTag("git-create-name").assertExists()
        val bounds = onNodeWithTag("git-create-branch").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 0 && bounds.right <= 220, "create should fit the sidebar: $bounds")
    }
}
