package app.thdev.moru.feature.git.screenshot

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.feature.git.history.GitHistoryList
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.feature.git.history.GitHistoryScreen
import app.thdev.moru.feature.git.history.layoutGitGraph
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test

/** Pins the commit graph list in both themes and in the compact layout for narrow panes. */
class GitHistoryScreenshotTest {
    private val comparator = GoldenImageComparator()

    @Test
    fun historyInLightTheme() = assertHistory("git_history_light", lightCkColors, widthDp = 1000)

    @Test
    fun historyInDarkTheme() = assertHistory("git_history_dark", darkCkColors, widthDp = 1000)

    @Test
    fun compactHistoryDropsAuthorAndHash() =
        assertHistory("git_history_compact_light", lightCkColors, widthDp = 560)

    @Test
    fun manyBranchesKeepEverySubjectReadableInANarrowPane() =
        assertHistory("git_history_many_branches_narrow_light", lightCkColors, widthDp = 420, commits = GitHistoryFixtures.manyBranches())

    @Test
    fun manyBranchesKeepEverySubjectReadableInAWidePane() =
        assertHistory("git_history_many_branches_wide_light", lightCkColors, widthDp = 1000, commits = GitHistoryFixtures.manyBranches())

    @Test
    fun screenShowsHeaderListAndSelectedCommitDetail() =
        assertScreen("git_history_screen_light", lightCkColors, GitHistoryResult.Loaded(GitHistoryFixtures.commits(), truncated = true))

    @Test
    fun screenSaysSoWhileLoading() = assertScreen("git_history_loading_light", lightCkColors, null)

    @Test
    fun screenExplainsAnUnavailableRepository() =
        assertScreen("git_history_unavailable_light", lightCkColors, GitHistoryResult.Unavailable("fatal: not a git repository"))

    @Test
    fun screenSaysSoForARepositoryWithoutCommits() =
        assertScreen("git_history_empty_light", lightCkColors, GitHistoryResult.Loaded(emptyList(), truncated = false))

    @Test
    fun screenInKorean() =
        assertScreen(
            "git_history_screen_ko_light", lightCkColors,
            GitHistoryResult.Loaded(GitHistoryFixtures.commits(), truncated = true), UiLanguage.Korean,
        )

    private fun assertScreen(
        name: String,
        colors: MoruColors,
        result: GitHistoryResult?,
        language: UiLanguage = UiLanguage.English,
    ) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = 1040, windowHeight = 560, language = language) {
            GitHistoryScreen(
                result = result,
                nowEpochSeconds = GitHistoryFixtures.NOW_EPOCH_SECONDS,
                onRefresh = {},
                modifier = Modifier.width(1000.dp).height(500.dp),
                initialSelectedHash = GitHistoryFixtures.selectedHash.takeIf { result is GitHistoryResult.Loaded },
            )
        }

    private fun assertHistory(
        name: String,
        colors: MoruColors,
        widthDp: Int,
        commits: List<GitCommit> = GitHistoryFixtures.commits(),
    ) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = widthDp + 40, windowHeight = 520) {
            GitHistoryList(
                rows = layoutGitGraph(commits),
                selectedHash = GitHistoryFixtures.selectedHash,
                nowEpochSeconds = GitHistoryFixtures.NOW_EPOCH_SECONDS,
                onSelect = {},
                modifier = Modifier.width(widthDp.dp).height(440.dp),
            )
        }
}
