package app.thdev.moru.feature.git

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitContextInteractionTest {
    @Test
    fun folderSwitchDropsApprovalAndDraftBeforeTheNewFolderCanExecute() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply {
            reader = { path -> GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(rootPath = path, revision = path)) }
        }
        var folder by mutableStateOf("/work/first task")
        setContent { MoruTheme { GitManagementScreen(folder, fake, NoHistory) } }
        awaitGitTag()
        onNodeWithTag("git-commit-message").performTextInput("Old folder draft")
        onNodeWithTag("git-commit").performClick()
        runOnIdle { folder = "/work/second task" }
        waitUntil(timeoutMillis = 10_000) { fake.reads.contains("/work/second task") }
        awaitGitTag()
        awaitGitIdle()
        onNodeWithTag("git-root-path").assertTextEquals("/work/second task")
        onNodeWithTag("git-approval").assertDoesNotExist()
        onNodeWithTag("git-commit").assertIsNotEnabled()
        onNodeWithTag("git-commit-message").performTextInput("New folder draft")
        onNodeWithTag("git-commit").performClick()
        onNodeWithText("Folder: /work/second task").assertExists()
        onNodeWithText("Message: New folder draft").assertExists()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle {
            assertEquals("/work/second task", fake.calls.single().folder)
            assertEquals("/work/second task", fake.calls.single().revision)
        }
    }

    @Test
    fun feedbackFromThePreviousFolderDoesNotLeakIntoTheNextFolder() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply { result = GitActionResult.Failed("Old folder is locked") }
        var folder by mutableStateOf(GitManagementFixtures.Folder)
        setContent { MoruTheme { GitManagementScreen(folder, fake, NoHistory) } }
        awaitGitTag()
        onNodeWithTag("git-stage-new file[1].txt").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { folder = "/work/new folder" }
        waitUntil(timeoutMillis = 10_000) { fake.reads.contains("/work/new folder") }
        awaitGitTag()
        awaitGitIdle()
        onNodeWithTag("git-feedback").assertDoesNotExist()
        onNodeWithTag("git-root-path").assertTextEquals("/work/new folder")
        onNodeWithTag("git-commit").assertIsNotEnabled()
    }

    @Test
    fun explicitRefreshInvalidatesAnUnsubmittedApproval() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-commit-message").performTextInput("Reviewed change")
        onNodeWithTag("git-commit").performClick()
        fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(revision = "new-r2"))
        onNodeWithTag("git-refresh").performClick()
        awaitGitIdle()
        onNodeWithTag("git-approval").assertDoesNotExist()
        runOnIdle { assertEquals(0, fake.calls.size) }
    }
}
