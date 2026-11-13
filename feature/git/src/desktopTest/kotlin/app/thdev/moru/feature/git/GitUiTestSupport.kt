package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.domain.GitRepository
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import java.io.File
import javax.imageio.ImageIO

/** A repository whose log is empty and whose commits have no diff, for tests that are not about history. */
internal val NoHistory = GitHistorySource(
    loadHistory = { GitHistoryResult.Loaded(emptyList(), truncated = false) },
    loadCommitDiff = { _, _ -> CommitDiffResult.Unavailable("No diff in this fixture.") },
)

/** The workspace body over [NoHistory], for stateless Content tests that need the body in place. */
internal fun statelessBody(
    state: GitManagementUiState,
    workspacePath: String,
): @Composable (Modifier) -> Unit = { bodyModifier ->
    GitWorkspaceBody(state, workspacePath, NoHistory, {}, bodyModifier)
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showGit(repository: GitRepository?, folder: String = GitManagementFixtures.Folder) {
    setContent { MoruTheme { GitManagementScreen(folder, repository, history = NoHistory) } }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitGitTag(tag: String = "git-commit-message") {
    waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitGitIdle() {
    waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-loading").fetchSemanticsNodes().isEmpty() }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.captureGit(name: String) {
    val target = File("build/ui-captures/$name.png")
    target.parentFile.mkdirs()
    ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", target)
}
