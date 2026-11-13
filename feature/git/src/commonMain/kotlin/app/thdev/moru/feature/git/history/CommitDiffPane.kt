package app.thdev.moru.feature.git.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.feature.git.i18n.HistoryStrings
import app.thdev.moru.feature.git.i18n.historyStrings
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.feature.git.history.diff.DiffFilesBrowser

/**
 * What one commit changed: who and when, the changed files, and the diff of the chosen file.
 * [diff] is null while it loads. Stateless apart from which file is open, which resets for a new [diff].
 */
@Composable
fun CommitDiffPane(
    commit: GitCommit,
    diff: CommitDiffResult?,
    modifier: Modifier = Modifier,
) {
    val strings = historyStrings()
    Column(modifier = modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page)) {
        CommitSummary(commit, strings)
        when (diff) {
            null -> Message(strings.diffLoading)
            is CommitDiffResult.Unavailable -> Message(diff.message)
            is CommitDiffResult.Loaded -> LoadedDiff(diff, strings)
        }
    }
}

@Composable
private fun LoadedDiff(diff: CommitDiffResult.Loaded, strings: HistoryStrings) {
    if (diff.files.isEmpty()) {
        Message(strings.noFileChanges)
        return
    }
    DiffFilesBrowser(diff)
}

@Composable
private fun CommitSummary(commit: GitCommit, strings: HistoryStrings) {
    Column(
        modifier = Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Panel).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MoruText(
            text = commit.subject,
            style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
        )
        MoruText(
            text = strings.authorLine(commit.authorName, commit.authorEmail, commit.isMerge, commit.parents.size),
            style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary,
            maxLines = 1,
        )
        MoruText(
            text = commit.hash,
            style = MoruTheme.typography.monospace.copy(fontSize = MoruTheme.typography.caption.fontSize),
            color = MoruTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun Message(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        MoruText(text = text, color = MoruTheme.colors.textSecondary)
    }
}
