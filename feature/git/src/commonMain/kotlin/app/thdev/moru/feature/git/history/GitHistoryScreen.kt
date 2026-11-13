package app.thdev.moru.feature.git.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.feature.git.i18n.HistoryStrings
import app.thdev.moru.feature.git.i18n.historyStrings
import app.thdev.moru.core.model.git.diff.CommitDiffResult
/**
 * The History screen. [result] is null while the history is loading, so the screen can
 * say so instead of showing an empty list. Selection is local view state and resets when
 * a new [result] arrives. [nowEpochSeconds] is passed in to keep ages deterministic.
 */
@Composable
fun GitHistoryScreen(
    result: GitHistoryResult?,
    nowEpochSeconds: Long,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedHash: String? = null,
    loadCommitDiff: ((String) -> CommitDiffResult)? = null,
) {
    val strings = historyStrings()
    var selectedHash by remember(result) { mutableStateOf(initialSelectedHash) }
    Column(modifier = modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page)) {
        HistoryHeader(result = result, onRefresh = onRefresh, strings = strings)
        when (result) {
            null -> StateMessage(strings.loading)
            is GitHistoryResult.Unavailable -> StateMessage(result.message)
            is GitHistoryResult.Loaded -> {
                if (result.commits.isEmpty()) {
                    StateMessage(strings.noCommits)
                } else {
                    val rows = remember(result) { layoutGitGraph(result.commits) }
                    val selectedCommit = result.commits.firstOrNull { commit -> commit.hash == selectedHash }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        GitHistoryList(
                            rows = rows,
                            selectedHash = selectedHash,
                            nowEpochSeconds = nowEpochSeconds,
                            onSelect = { hash -> selectedHash = hash },
                            modifier = Modifier.weight(if (selectedCommit == null) 1f else ListShare).fillMaxHeight(),
                        )
                        if (selectedCommit != null) {
                            SelectedCommitDiff(
                                commit = selectedCommit,
                                loadCommitDiff = loadCommitDiff,
                                modifier = Modifier.weight(DiffShare).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(result: GitHistoryResult?, onRefresh: () -> Unit, strings: HistoryStrings) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MoruText(text = strings.title, style = MoruTheme.typography.h2)
            MoruText(
                text = summaryOf(result, strings),
                style = MoruTheme.typography.caption,
                color = MoruTheme.colors.textSecondary,
            )
        }
        MoruButton(text = strings.refresh, onClick = onRefresh, type = MoruButtonType.SECONDARY)
    }
}

private fun summaryOf(result: GitHistoryResult?, strings: HistoryStrings): String =
    when (result) {
        is GitHistoryResult.Loaded -> strings.commitCount(result.commits.size, result.truncated)
        else -> " "
    }

@Composable
private fun StateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        MoruText(text = message, color = MoruTheme.colors.textSecondary)
    }
}

private const val ListShare = 0.42f
private const val DiffShare = 0.58f
