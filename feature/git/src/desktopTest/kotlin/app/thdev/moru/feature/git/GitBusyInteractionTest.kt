package app.thdev.moru.feature.git

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.model.git.GitActionResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@OptIn(ExperimentalTestApi::class)
class GitBusyInteractionTest {
    @Test
    fun initialReadRunsAwayFromTheUiThreadAndLoadingDisablesActions() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        var uiThread: Thread? = null
        var readThread: Thread? = null
        runOnIdle { uiThread = Thread.currentThread() }
        val fake = FakeGitRepository().apply {
            beforeRead = {
                readThread = Thread.currentThread()
                started.countDown()
                check(release.await(10, TimeUnit.SECONDS))
            }
        }
        try {
            showGit(fake)
            waitUntil(timeoutMillis = 10_000) { started.count == 0L }
            onNodeWithTag("git-loading").assertExists()
            onNodeWithTag("git-refresh").assertIsNotEnabled()
            onNodeWithTag("git-fetch").assertIsNotEnabled()
            assertNotSame(uiThread, readThread)
        } finally {
            release.countDown()
        }
        awaitGitTag()
        awaitGitIdle()
    }

    @Test
    fun busyPreventsDuplicateIndexActionsAndRefreshesAfterFailure() = runDesktopComposeUiTest(width = 1100, height = 800) {
        val release = CountDownLatch(1)
        val fake = FakeGitRepository().apply {
            onExecute = {
                check(release.await(10, TimeUnit.SECONDS))
                GitActionResult.Failed("Index is locked")
            }
        }
        showGit(fake)
        awaitGitTag()
        try {
            onNodeWithTag("git-stage-new file[1].txt").performClick()
            waitUntil(timeoutMillis = 10_000) { fake.calls.size == 1 }
            onNodeWithTag("git-stage-new file[1].txt").assertIsNotEnabled().performClick()
            onNodeWithTag("git-refresh").assertIsNotEnabled()
            onNodeWithTag("git-commit-message").assertIsNotEnabled()
            runOnIdle { assertEquals(1, fake.calls.size) }
        } finally {
            release.countDown()
        }
        awaitGitTag("git-feedback")
        awaitGitIdle()
        onNodeWithTag("git-feedback").assertTextContains("Failed: Index is locked")
        runOnIdle {
            assertEquals(1, fake.calls.size)
            assertEquals(2, fake.reads.size)
        }
    }
}
