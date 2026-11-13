package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.ui.diff.DiffFileRow
import app.thdev.moru.core.ui.i18n.coreUiStrings
import app.thdev.moru.feature.git.i18n.historyStrings

/**
 * What a chosen commit changed, in the right column where the changes to stage otherwise sit: its subject, author and
 * hash, then only the list of changed files. Choosing a file opens its diff in place of the history ([onOpenFile]);
 * [openPath] is the file shown there. [diff] is null while it loads.
 */
@Composable
internal fun GitCommitFilesPanel(
    commit: GitCommit,
    diff: CommitDiffResult?,
    openPath: String?,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = historyStrings()
    Column(modifier.fillMaxSize().testTag("git-commit-files")) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 180.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoruText(commit.subject, style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold))
            MoruText(strings.authorLine(commit.authorName, commit.authorEmail, commit.isMerge, commit.parents.size),
                style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary, maxLines = 1)
            MoruText(commit.hash, style = MoruTheme.typography.monospace.copy(fontSize = MoruTheme.typography.caption.fontSize),
                color = MoruTheme.colors.textSecondary, maxLines = 1)
        }
        when (diff) {
            null -> Message(strings.diffLoading)
            is CommitDiffResult.Unavailable -> Message(diff.message)
            is CommitDiffResult.Loaded -> if (diff.files.isEmpty()) Message(strings.noFileChanges) else {
                MoruText(
                    coreUiStrings().filesSummary(diff.files.size, diff.files.sumOf { it.additions },
                        diff.files.sumOf { it.deletions }, diff.truncated),
                    Modifier.fillMaxWidth().ckSurface(MoruSurfaceRole.Panel).padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                )
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(diff.files, key = { it.displayPath }) { file ->
                        DiffFileRow(file, selected = file.displayPath == openPath,
                            onClick = { onOpenFile(file.displayPath) },
                            modifier = Modifier.testTag("git-commit-file-${file.displayPath}"))
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        MoruText(text, color = MoruTheme.colors.textSecondary)
    }
}
