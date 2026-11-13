package app.thdev.moru.feature.git

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitUnavailableInteractionTest {
    @Test
    fun missingAdapterShowsUnavailableAndCannotProposeNetworkActions() = runDesktopComposeUiTest {
        showGit(null)
        awaitGitTag("git-unavailable")
        onNodeWithTag("git-unavailable").assertTextContains("unavailable", substring = true)
        onNodeWithTag("git-fetch").assertIsNotEnabled()
        onNodeWithTag("git-push").assertIsNotEnabled()
        onNodeWithTag("git-commit-message").assertDoesNotExist()
    }

    @Test
    fun blankFolderDoesNotReadTheAdapterOrFallBackToAnotherFolder() = runDesktopComposeUiTest {
        val fake = FakeGitRepository()
        showGit(fake, folder = "")
        awaitGitTag("git-unavailable")
        onNodeWithTag("git-unavailable").assertTextContains("No folder is selected", substring = true)
        onNodeWithTag("git-fetch").assertIsNotEnabled()
        runOnIdle { assertEquals(0, fake.reads.size) }
    }

    @Test
    fun missingFolderReasonIsVisibleAndReadTargetsOnlyTheRequestedFolder() = runDesktopComposeUiTest {
        val fake = FakeGitRepository().apply { readState = GitRepositoryState.Unavailable("Folder does not exist: /missing") }
        showGit(fake, folder = "/missing")
        awaitGitTag("git-unavailable")
        onNodeWithTag("git-unavailable").assertTextContains("Folder does not exist: /missing")
        runOnIdle { assertEquals(listOf("/missing"), fake.reads.toList()) }
    }

    @Test
    fun detachedHeadCannotPushAndAnUnbornBranchCanCommitItsStagedIndex() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply {
            readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(branchName = null, upstream = null))
        }
        showGit(fake)
        awaitGitTag()
        onNodeWithContentDescription("Detached HEAD", substring = true).assertExists()
        onNodeWithTag("git-push").assertIsNotEnabled()
        fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(head = null, branchName = "main", upstream = null, remotes = emptyList()))
        onNodeWithTag("git-refresh").performClick()
        awaitGitIdle()
        onNodeWithContentDescription("Branch: main (no commits yet)").assertExists()
        onNodeWithTag("git-commit-message").performTextInput("First commit")
        onNodeWithTag("git-commit").performClick()
        onNodeWithText("Branch: main (no commits yet)").assertExists()
        onNodeWithTag("git-cancel").performClick()
        runOnIdle { assertEquals(0, fake.calls.size) }
    }
}
