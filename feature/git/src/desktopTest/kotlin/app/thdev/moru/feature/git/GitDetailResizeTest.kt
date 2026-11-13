package app.thdev.moru.feature.git

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitDetailResizeTest {
    @Test
    fun draggingTheDetailPaneChangesItsWidthAndSwitchingFolderResetsIt() = runDesktopComposeUiTest(width = 1400, height = 760) {
        val repo = FakeGitRepository().apply {
            reader = { GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(rootPath = it)) }
        }
        var folder by mutableStateOf(GitManagementFixtures.Folder)
        setContent { MoruTheme { GitManagementScreen(folder, repo, NoHistory) } }
        awaitGitTag()
        val before = onNodeWithTag("git-commit-message").fetchSemanticsNode().boundsInRoot.width
        val divider = onNodeWithContentDescription("Resize Git detail pane")
        divider.performMouseInput {
            moveTo(center); press(); moveTo(center + Offset(-140f, 0f), delayMillis = 100); release()
        }
        waitForIdle()
        assertTrue(onNodeWithTag("git-commit-message").fetchSemanticsNode().boundsInRoot.width > before + 100)
        captureGit("git-detail-resized")
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { pressKey(Key.Home) }
        waitForIdle()
        assertEquals(before, onNodeWithTag("git-commit-message").fetchSemanticsNode().boundsInRoot.width, 1f)
        divider.performKeyInput { repeat(4) { pressKey(Key.DirectionLeft) } }
        runOnIdle { folder = "/work/other folder" }
        awaitGitTag()
        awaitGitIdle()
        assertEquals(before, onNodeWithTag("git-commit-message").fetchSemanticsNode().boundsInRoot.width, 1f)
        runOnIdle { assertTrue(repo.calls.isEmpty(), "Resizing must not execute Git operations") }
    }

    @Test
    fun aSavedDetailWidthIsRestoredAndEachResizeIsReported() = runDesktopComposeUiTest(width = 1400, height = 760) {
        val repo = FakeGitRepository().apply {
            reader = { GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(rootPath = it)) }
        }
        val reported = mutableListOf<Dp?>()
        setContent {
            MoruTheme {
                GitManagementScreen(GitManagementFixtures.Folder, repo, NoHistory, detailWidth = 560.dp, onDetailWidthChange = { reported += it })
            }
        }
        awaitGitTag()
        // One step from the restored 560dp proves the saved width, not the window default, was applied.
        val divider = onNodeWithContentDescription("Resize Git detail pane")
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { pressKey(Key.DirectionLeft) }
        divider.performKeyInput { pressKey(Key.Home) }
        runOnIdle { assertEquals(listOf<Dp?>(584.dp, null), reported) }
    }

    @Test
    fun compactNavigationRetainsSinglePaneActions() = runDesktopComposeUiTest(width = 520, height = 760) {
        val repository = FakeGitRepository()
        showGit(repository)
        awaitGitTag()
        onNodeWithContentDescription("Detach Git detail pane").assertIsDisplayed().assertHasClickAction()
        onNodeWithTag("git-commit-message").performTextInput("Draft stays with this repository")
        onNodeWithContentDescription("Resize Git detail pane").assertDoesNotExist()
        onNodeWithText("History", useUnmergedTree = true).performClick()
        onNodeWithTag("git-wip-row").assertIsDisplayed()
        onNodeWithTag("git-commit-message").assertDoesNotExist()
        onNodeWithContentDescription("Detach Git detail pane").assertDoesNotExist()
        onNodeWithText("Changes").performClick()
        awaitGitTag()
        onNodeWithTag("git-commit-message").assertTextContains("Draft stays with this repository")
        runOnIdle { assertTrue(repository.calls.isEmpty(), "Panel navigation must not execute Git operations") }
        captureGit("git-detail-compact")
    }
}
