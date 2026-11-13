package app.thdev.moru.feature.git

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitCommitInteractionTest {
    @Test
    fun cancelExecutesNothingAndConfirmCommitsOnlyTheReviewedIndex() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-commit-message").performTextInput("Commit the staged change")
        onNodeWithTag("git-commit").performClick()
        onNodeWithText("Folder: ${GitManagementFixtures.Folder}").assertExists()
        onNodeWithText("Branch: topic").assertExists()
        onNodeWithText("src/staged.kt").assertExists()
        onNodeWithText("src/unstaged.kt").assertDoesNotExist()
        captureGit("git-commit-approval")
        onNodeWithTag("git-cancel").performClick()
        runOnIdle { assertEquals(emptyList(), fake.calls.toList()) }
        onNodeWithTag("git-commit").performClick()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle {
            assertEquals(listOf(GitCall(GitManagementFixtures.Folder, "reviewed-r1", GitAction.Commit("Commit the staged change"))), fake.calls.toList())
            assertEquals(2, fake.reads.size)
        }
        onNodeWithTag("git-feedback").assertTextContains("Succeeded", substring = true)
    }

    @Test
    fun stageAndUnstageKeepLiteralSelectedRenamePathAndUseTheRevisionFromEachRead() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        val original = "old file[1].txt"
        val path = "new file*.txt"
        val renamed = GitManagementFixtures.unstaged.copy(path = path, originalPath = original, worktreeStatus = 'R')
        fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(files = listOf(renamed)))
        fake.onExecute = { call ->
            val staged = call.action is GitAction.Stage
            fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(
                revision = if (staged) "staged-r2" else "unstaged-r3",
                files = listOf(renamed.copy(indexStatus = if (staged) 'R' else ' ', worktreeStatus = if (staged) ' ' else 'R')),
            ))
            GitActionResult.Succeeded("Index updated")
        }
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-stage-$path").performClick()
        awaitGitTag("git-unstage-$path")
        awaitGitIdle()
        onNodeWithTag("git-unstage-$path").performClick()
        awaitGitTag("git-stage-$path")
        awaitGitIdle()
        runOnIdle {
            assertEquals(listOf(
                GitCall(GitManagementFixtures.Folder, "reviewed-r1", GitAction.Stage(listOf(path))),
                GitCall(GitManagementFixtures.Folder, "staged-r2", GitAction.Unstage(listOf(path))),
            ), fake.calls.toList())
            assertEquals(3, fake.reads.size)
        }
        onNodeWithTag("git-approval").assertDoesNotExist()
    }

    @Test
    fun staleApprovalIsRejectedAndRefreshKeepsTheFailureVisible() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-commit-message").performTextInput("Reviewed message")
        onNodeWithTag("git-commit").performClick()
        fake.result = GitActionResult.Rejected("Repository changed. Review a fresh snapshot.")
        fake.readState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(revision = "external-r2"))
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-feedback").assertTextContains("Rejected: Repository changed", substring = true)
        onNodeWithTag("git-approval").assertDoesNotExist()
        runOnIdle {
            assertEquals("reviewed-r1", fake.calls.single().revision)
            assertEquals(2, fake.reads.size)
        }
        onNodeWithTag("git-commit").performClick()
        onNodeWithTag("git-confirm").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle { assertEquals("external-r2", fake.calls.last().revision) }
    }

    @Test
    fun indexBulkButtonsUseOnlyThePathsShownInTheReviewedSnapshot() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository()
        showGit(fake)
        awaitGitTag()
        onNodeWithTag("git-stage-all").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-unstage-all").performClick()
        awaitGitTag("git-feedback")
        awaitGitIdle()
        runOnIdle {
            assertEquals(listOf(
                GitAction.Stage(listOf("src/unstaged.kt", "new file[1].txt")),
                GitAction.Unstage(listOf("src/staged.kt")),
            ), fake.calls.map { it.action })
        }
    }
}
