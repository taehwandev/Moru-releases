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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitRepositoryState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.designsystem.theme.MoruTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitSyncStateTest {
    @Test
    fun targetsExcludeSymbolicHeadAndOtherRemotesButOfferFirstPublication() {
        val original = GitManagementFixtures.snapshot
        val snapshot = original.copy(branches = original.branches + listOf(
            GitBranchInfo("origin/HEAD", isRemote = true),
            GitBranchInfo("origin/release/stable", isRemote = true),
            GitBranchInfo("upstream/private", isRemote = true),
        ))
        assertEquals(listOf("release/stable", "topic"), snapshot.remoteTargets("origin"))
        assertEquals(listOf("private", "topic"), snapshot.remoteTargets("upstream"))
        assertEquals(emptyList(), snapshot.remoteTargets("missing"))
    }

    @Test
    fun changingFoldersResetsTransientBranchSearchAndNameForms() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val fake = FakeGitRepository().apply {
            reader = { folder -> GitRepositoryState.Loaded(GitManagementFixtures.snapshot.copy(rootPath = folder)) }
        }
        var folder by mutableStateOf("/work/first")
        setContent { MoruTheme { GitManagementScreen(folder, fake, NoHistory) } }
        awaitGitTag()
        onNodeWithTag("git-branch-search").performTextInput("reviewed")
        onNodeWithTag("git-new-branch").performClick()
        onNodeWithTag("git-create-name").assertExists()
        runOnIdle { folder = "/work/second" }
        waitUntil(timeoutMillis = 10_000) { fake.reads.contains("/work/second") }
        awaitGitTag()
        awaitGitIdle()
        onNodeWithTag("git-create-name").assertDoesNotExist()
        onNodeWithTag("git-select-branch-topic").assertExists()
    }

    @Test
    fun syncBusyBlocksDuplicatePushAndRefreshesRejectedState() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val release = CountDownLatch(1)
        val fake = FakeGitRepository().apply {
            onExecute = {
                check(release.await(10, TimeUnit.SECONDS))
                GitActionResult.Rejected("Repository changed. Refresh and retry.")
            }
        }
        showGit(fake)
        awaitGitTag()
        try {
            onNodeWithTag("git-push").performClick()
            waitUntil(timeoutMillis = 10_000) { fake.calls.size == 1 }
            onNodeWithTag("git-push").assertIsNotEnabled().performClick()
            onNodeWithTag("git-fetch").assertIsNotEnabled()
            onNodeWithTag("git-branch-picker").assertIsNotEnabled()
            runOnIdle { assertEquals(1, fake.calls.size) }
        } finally {
            release.countDown()
        }
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-confirm").assertDoesNotExist()
        onNodeWithTag("git-feedback").assertTextContains("Rejected: Repository changed.", substring = true)
        runOnIdle {
            assertEquals(GitAction.Push("origin", "topic"), fake.calls.single().action)
            assertEquals(2, fake.reads.size)
        }
    }

    @Test
    fun compactWindowKeepsBranchAndHistoryAccessWithoutClippingChanges() = runDesktopComposeUiTest(width = 520, height = 760) {
        showGit(FakeGitRepository())
        awaitGitTag()
        onNodeWithTag("git-push").assertIsDisplayed()
        onNodeWithTag("git-commit-message").assertIsDisplayed()
        onNodeWithText("Branches").performClick()
        onNodeWithTag("git-branches-popup").assertIsDisplayed()
        onNodeWithTag("git-select-branch-reviewed").performMouseInput { rightClick() }
        onNodeWithTag("git-switch-branch").assertIsNotEnabled()
        onNodeWithTag("git-menu-copy").performClick()
        val popupCapture = File("build/ui-captures/git-compact-branch-browser.png").apply { parentFile.mkdirs() }
        ImageIO.write(onNodeWithTag("git-branches-popup").captureToImage().toAwtImage(), "png", popupCapture)
        onNodeWithText("Done").performClick()
        onNodeWithTag("git-branches-popup").assertDoesNotExist()
        onNodeWithText("History").performClick()
        onNodeWithTag("git-commit-message").assertDoesNotExist()
        onNodeWithText("Changes").performClick()
        onNodeWithTag("git-commit-message").assertIsDisplayed()
        captureGit("git-compact")
    }
}
