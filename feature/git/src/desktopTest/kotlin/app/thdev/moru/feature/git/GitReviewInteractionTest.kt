package app.thdev.moru.feature.git

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.git.GitRepositoryState
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffHunk
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.history.GitHistoryResult
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GitReviewInteractionTest {
    @Test
    fun workingFilesAndCommitsOpenTheirOwnDiffWithoutStaging() = runDesktopComposeUiTest(width = 1400, height = 760) {
        val repo = FakeGitRepository()
        setContent { MoruTheme(colors = lightCkColors) { GitManagementScreen(GitManagementFixtures.Folder, repo, reviewHistory()) } }
        awaitGitTag()
        // A working file's diff takes the history's place; the changes stay beside it.
        onNodeWithTag("git-review-src/unstaged.kt").performClick()
        awaitText("current working line")
        onNodeWithTag("git-wip-row").assertDoesNotExist()
        onNodeWithTag("git-commit-message").assertIsDisplayed()
        captureGit("git-working-diff-light")
        onNodeWithTag("git-file-diff-close").performClick()
        onNodeWithTag("git-wip-row").assertIsDisplayed()

        // Choosing a commit lists its files on the right and leaves the history in view.
        onNodeWithText("Review commit", useUnmergedTree = true).performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-commit-file-src/committed.kt").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("git-wip-row").assertIsDisplayed()
        onNodeWithTag("git-commit-message").assertDoesNotExist()
        onNodeWithText("committed line").assertDoesNotExist()
        captureGit("git-commit-files-light")

        // Its file opens over the history, with the file list still there to pick from.
        onNodeWithTag("git-commit-file-src/committed.kt").performClick()
        awaitText("committed line")
        onNodeWithTag("git-wip-row").assertDoesNotExist()
        onNodeWithTag("git-commit-files").assertIsDisplayed()
        captureGit("git-commit-diff-light")
        onNodeWithTag("git-file-diff-close").performClick()
        onNodeWithTag("git-wip-row").assertIsDisplayed()
        onNodeWithTag("git-detail-close").performClick()
        onNodeWithTag("git-commit-message").assertIsDisplayed()
        onNodeWithTag("git-review-src/staged.kt").performClick()
        awaitText("staged working line")
        runOnIdle { assertTrue(repo.calls.isEmpty(), "Reviewing diffs must not execute Git actions") }
    }

    @Test
    fun compactDarkGitKeepsHistoryAndWorkingReviewAccessible() = runDesktopComposeUiTest(width = 520, height = 760) {
        val repo = FakeGitRepository()
        setContent { MoruTheme(colors = darkCkColors) { GitManagementScreen(GitManagementFixtures.Folder, repo, reviewHistory()) } }
        awaitGitTag()
        onNodeWithTag("git-review-src/unstaged.kt").performClick()
        awaitText("current working line")
        captureGit("git-working-diff-compact-dark")
        onNodeWithTag("git-file-diff-close").performClick()
        // A narrow history keeps its rows wide and scrolls sideways instead of cutting the subjects.
        val rowsWidth = onNodeWithTag("git-history-list").fetchSemanticsNode().size.width
        assertTrue(rowsWidth >= with(density) { 880.dp.roundToPx() }, "history rows must keep their width, got $rowsWidth")
        onNodeWithText("Review commit", useUnmergedTree = true).performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-commit-file-src/committed.kt").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("git-commit-file-src/committed.kt").performClick()
        awaitText("committed line")
        onNodeWithTag("git-file-diff-close").performClick()
        onNodeWithTag("git-wip-row").performClick()
        onNodeWithTag("git-commit-message").assertExists()
        runOnIdle { assertTrue(repo.calls.isEmpty()) }
    }

    @Test
    fun switchingFolderClearsTheOpenWorkingFile() = runDesktopComposeUiTest(width = 1400, height = 760) {
        val repo = FakeGitRepository().apply {
            reader = { GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(rootPath = it)) }
        }
        var folder by mutableStateOf(GitManagementFixtures.Folder)
        setContent { MoruTheme { GitManagementScreen(folder, repo, reviewHistory()) } }
        awaitGitTag()
        onNodeWithTag("git-review-src/unstaged.kt").performClick()
        awaitText("current working line")
        runOnIdle { folder = "/work/second folder" }
        awaitGitTag()
        onNodeWithTag("git-file-diff").assertDoesNotExist()
        onNodeWithText("current working line").assertDoesNotExist()
        onNodeWithTag("git-root-path").assertTextEquals(folder)
    }

    @Test
    fun aMissingPatchExplainsWhyInsteadOfShowingAnotherFile() = runDesktopComposeUiTest(width = 1400, height = 760) {
        setContent { MoruTheme { GitManagementScreen(GitManagementFixtures.Folder, FakeGitRepository(), reviewHistory()) } }
        awaitGitTag()
        onNodeWithTag("git-review-new file[1].txt").performClick()
        awaitText("No current patch for this file. Refresh Git to update the file list.")
        onNodeWithText("current working line").assertDoesNotExist()
        onNodeWithTag("git-file-diff-close").performClick()
        onNodeWithTag("git-wip-row").assertIsDisplayed()
    }

    private fun ComposeUiTest.awaitText(text: String) {
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(text).assertIsDisplayed()
    }

    private fun reviewHistory() = GitHistorySource(
        loadHistory = { GitHistoryResult.Loaded(listOf(GitCommit("abc123456", emptyList(), "Reviewer", "dev@example.com", 1, emptyList(), "Review commit")), false) },
        loadCommitDiff = { _, _ -> patch("src/committed.kt", "committed line") },
        loadWorkingChanges = { CommitDiffResult.Loaded(
            patch("src/unstaged.kt", "current working line").files + patch("src/staged.kt", "staged working line").files, false,
        ) },
    )

    private fun patch(path: String, text: String) = CommitDiffResult.Loaded(listOf(
        DiffFile(path, path, DiffFileStatus.Modified, listOf(DiffHunk("@@ -1 +1 @@", listOf(
            DiffLine(DiffLineKind.Added, text, null, 1),
        ))), false),
    ), false)
}
