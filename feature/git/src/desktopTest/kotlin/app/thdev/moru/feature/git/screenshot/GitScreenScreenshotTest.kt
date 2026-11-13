package app.thdev.moru.feature.git.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import app.thdev.moru.core.designsystem.theme.UiLanguage
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.git.GitRepositoryState
import app.thdev.moru.feature.git.GitHistorySource
import app.thdev.moru.feature.git.GitManagementContent
import app.thdev.moru.feature.git.GitManagementFixtures
import app.thdev.moru.feature.git.GitManagementUiState
import app.thdev.moru.feature.git.afterRead
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.data.git.workbench.parseGitDiff
import kotlin.test.Test

/**
 * Pins the Git tab as one screen: branches on the left, the commit graph with its messages in the middle, and the
 * working changes or a chosen commit's diff on the right.
 */
@OptIn(ExperimentalTestApi::class)
class GitScreenScreenshotTest {
    private val comparator = GoldenImageComparator()

    private val source = GitHistorySource(
        loadHistory = { GitHistoryResult.Loaded(GitHistoryFixtures.commits(), truncated = false) },
        loadCommitDiff = { _, _ -> CommitDiffResult.Loaded(files = parseGitDiff(SAMPLE_PATCH), truncated = false) },
    )

    @Test
    fun workingChangesAreSelectedFirst() = assertScreen("git_screen_working_light", lightCkColors, openCommit = false)

    @Test
    fun aChosenCommitShowsItsDiffBesideTheGraph() = assertScreen("git_screen_commit_light", lightCkColors, openCommit = true)

    @Test
    fun theWholeScreenInDarkTheme() = assertScreen("git_screen_commit_dark", darkCkColors, openCommit = true)

    @Test
    fun theWholeScreenInKorean() = assertScreen("git_screen_working_ko_light", lightCkColors, openCommit = false, language = UiLanguage.Korean)

    private fun assertScreen(
        name: String,
        colors: MoruColors,
        openCommit: Boolean,
        language: UiLanguage = UiLanguage.English,
    ) = runDesktopComposeUiTest(width = 1400, height = 760) {
        val state = GitManagementUiState().afterRead(GitRepositoryState.Loaded(GitManagementFixtures.snapshot))
        setContent {
            MoruTheme(darkTheme = !colors.isLight, colors = colors, materialStyle = MoruMaterialStyle.Apple) {
                CompositionLocalProvider(LocalUiLanguage provides language) {
                    Box(Modifier.fillMaxSize().background(colors.background)) {
                        GitManagementContent(state, GitManagementFixtures.Folder, source, Modifier.fillMaxSize(), onEvent = {})
                    }
                }
            }
        }
        val firstCommit = hasContentDescription("Merge pull request #3", substring = true)
        waitUntil(timeoutMillis = 10_000) { onAllNodes(firstCommit).fetchSemanticsNodes().isNotEmpty() }
        if (openCommit) {
            onAllNodes(hasContentDescription("feat(history): draw the commit graph", substring = true))[0].performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("LoginViewModel.kt", substring = true).fetchSemanticsNodes().isNotEmpty() }
            waitForIdle()
        }
        comparator.assertMatches(name, onRoot().captureToImage().toAwtImage())
    }
}
