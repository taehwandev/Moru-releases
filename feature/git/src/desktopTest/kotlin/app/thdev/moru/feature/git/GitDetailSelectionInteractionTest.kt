package app.thdev.moru.feature.git

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitRepositoryState
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlin.test.Test
import kotlin.test.assertEquals

/** The attached host still uses the selected commit and the current revision/event callback. */
@OptIn(ExperimentalTestApi::class)
class GitDetailSelectionInteractionTest {
    @Test
    fun selectingACommitAndReturningToChangesKeepsCurrentEventsAndDraft() = runDesktopComposeUiTest(width = 640, height = 760) {
        var selectedHash by mutableStateOf<String?>(null)
        var openPath by mutableStateOf<String?>(null)
        var state by mutableStateOf(GitManagementUiState()
            .afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot))
            .copy(commitMessage = "Repository draft"))
        val commit = GitCommit("abcdef0123456789", emptyList(), "Author", "author@example.test", 1, emptyList(), "Selected change")
        val firstEvents = mutableListOf<GitManagementEvent>()
        val currentEvents = mutableListOf<GitManagementEvent>()
        var useCurrentEvents by mutableStateOf(false)
        setContent {
            MoruTheme {
                GitDetachableDetailColumn(state, GitHistoryResult.Loaded(listOf(commit), false), selectedHash,
                    GitManagementFixtures.Folder, onEvent = { (if (useCurrentEvents) currentEvents else firstEvents).add(it) },
                    commitDiff = CommitDiffResult.Unavailable("Fixture diff"),
                    openPath = openPath,
                    onOpenFile = { openPath = it },
                    detached = false, onDetach = {}, onRestore = {})
            }
        }
        onNodeWithText("Changes", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("git-commit-message").assertTextContains("Repository draft")
        onNodeWithTag("git-review-${GitManagementFixtures.unstaged.path}").performClick()
        runOnIdle { assertEquals(GitManagementFixtures.unstaged.path, openPath) }
        onNodeWithTag("git-commit-message").assertTextContains("Repository draft")
        runOnIdle { assertEquals(emptyList(), firstEvents) }
        runOnIdle { selectedHash = commit.hash; openPath = null }
        onNodeWithText("Commit · abcdef0").assertIsDisplayed()
        onNodeWithText("Fixture diff").assertIsDisplayed()
        onNodeWithTag("git-commit-message").assertDoesNotExist()
        runOnIdle {
            selectedHash = null
            useCurrentEvents = true
            state = state.afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(revision = "fresh-r2")))
        }
        onNodeWithTag("git-commit-message").assertTextContains("Repository draft")
        onNodeWithTag("git-stage-${GitManagementFixtures.unstaged.path}").performClick()
        runOnIdle {
            assertEquals(emptyList(), firstEvents)
            assertEquals(listOf<GitManagementEvent>(GitManagementEvent.ChangeIndex(
                GitAction.Stage(listOf(GitManagementFixtures.unstaged.path)), "fresh-r2")), currentEvents)
        }
        captureGit("git-detail-selection-current")
    }
}
