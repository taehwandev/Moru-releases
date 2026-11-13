package app.thdev.moru.feature.git

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlin.test.Test
import kotlin.test.assertEquals

/** Headless fixture renders supply preview coverage without adding tooling dependencies. */
@OptIn(ExperimentalTestApi::class)
class GitStatelessContentTest {
    @Test
    fun mixedChangesRetainLiteralNamesAndBothPartialStagingControls() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val partial = GitManagementFixtures.staged.copy(path = "partial [file].kt", worktreeStatus = 'M')
        val conflict = GitManagementFixtures.unstaged.copy(path = "conflict.kt", indexStatus = 'U', worktreeStatus = 'U')
        val snapshot = GitManagementFixtures.snapshot.copy(files = listOf(partial, conflict, GitManagementFixtures.untracked))
        val state = GitManagementUiState().afterRead(GitRepositoryState.Loaded(snapshot))
        val events = mutableListOf<GitManagementEvent>()
        setContent { MoruTheme { GitManagementContent(state, snapshot.rootPath, NoHistory, onEvent = { events += it }) } }
        onNodeWithTag("git-review-partial [file].kt").assertTextContains("partial [file].kt")
        onNodeWithTag("git-stage-partial [file].kt").assertExists()
        onNodeWithTag("git-unstage-partial [file].kt").assertExists()
        onNodeWithText("Conflict (UU)").assertExists()
        onNodeWithText("Untracked").assertExists()
        onNodeWithTag("git-commit").assertIsNotEnabled()
        captureGit("git-mixed-changes")
        onNodeWithTag("git-refresh").performClick()
        runOnIdle { assertEquals(listOf<GitManagementEvent>(GitManagementEvent.Refresh), events.toList()) }
    }

    @Test
    fun theWorkingFolderRowAndTheBranchesAreOnTheSameScreenAsTheChanges() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val state = GitManagementUiState().afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot))
        setContent { MoruTheme { GitManagementContent(state, GitManagementFixtures.Folder, NoHistory, onEvent = {}) } }
        waitForIdle()
        onNodeWithTag("git-wip-row").assertIsDisplayed()
        onNodeWithTag("git-branches").assertIsDisplayed()
        onNodeWithTag("git-files").assertIsDisplayed()
        onNodeWithTag("git-commit").assertExists()
    }

    @Test
    fun compactCleanStateShowsEmptyChangesAndNoRemote() = runDesktopComposeUiTest(width = 420, height = 800) {
        val snapshot = GitManagementFixtures.snapshot.copy(files = emptyList(), remotes = emptyList(),
            upstream = null, upstreamRemote = null, upstreamBranch = null, ahead = null, behind = null)
        val state = GitManagementUiState().afterRead(GitRepositoryState.Loaded(snapshot))
        setContent { MoruTheme { GitManagementContent(state, snapshot.rootPath, NoHistory, onEvent = {}) } }
        onNodeWithText("Working folder is clean. No changes to stage.").assertExists()
        onNodeWithText("No remote configured. Local changes and branches are available.").assertExists()
        onNodeWithTag("git-stage-all").assertIsNotEnabled()
        onNodeWithTag("git-commit-message").assertExists()
        captureGit("git-compact-clean")
    }

    @Test
    fun loadingFixtureDisablesRefreshAndShowsItsReadState() = runDesktopComposeUiTest(width = 420, height = 600) {
        setContent { MoruTheme { GitManagementContent(GitManagementUiState(), GitManagementFixtures.Folder, NoHistory, onEvent = {}) } }
        onNodeWithTag("git-loading").assertTextContains("Loading repository", substring = true)
        onNodeWithTag("git-refresh").assertIsNotEnabled()
    }

    @Test
    fun unavailableFixtureShowsTheExactReason() = runDesktopComposeUiTest(width = 420, height = 600) {
        val state = GitManagementUiState(repositoryState = GitRepositoryState.Unavailable("Not a Git repository"), loading = false)
        setContent { MoruTheme { GitManagementContent(state, GitManagementFixtures.Folder, NoHistory, onEvent = {}) } }
        onNodeWithTag("git-unavailable").assertTextContains("Not a Git repository")
    }

    @Test
    fun branchActionsAreReachableFromTheRefRightClickMenu() = runDesktopComposeUiTest(width = 1100, height = 700) {
        val state = GitManagementUiState().afterRead(
            GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(files = emptyList())),
        ).copy(selectedBranch = "local:reviewed")
        setContent { MoruTheme { GitManagementContent(state, GitManagementFixtures.Folder, NoHistory, onEvent = {}) } }
        onNodeWithTag("git-switch-branch").assertDoesNotExist()
        captureGit("git-compact-branches")
        onNodeWithTag("git-select-branch-reviewed").performMouseInput { rightClick() }
        for (tag in listOf("git-switch-branch", "git-rename-open", "git-merge-branch", "git-delete-branch")) {
            onNodeWithTag(tag).assertIsDisplayed()
        }
        onNodeWithTag("git-rename-name").assertDoesNotExist()
    }
}
