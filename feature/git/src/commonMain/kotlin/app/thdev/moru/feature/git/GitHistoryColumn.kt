package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitWorkingFile
import app.thdev.moru.feature.git.history.GitGraphRow
import app.thdev.moru.feature.git.history.GitHistoryList
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.feature.git.history.layoutGitGraph
import app.thdev.moru.feature.git.i18n.historyStrings
import kotlin.time.Clock

/**
 * The middle column: the working-folder row, then the commit graph with every subject. [selectedHash] is null while
 * the working folder is the selection. [history] is null while it loads.
 */
@Composable
internal fun GitHistoryColumn(
    history: GitHistoryResult?,
    files: List<GitWorkingFile>,
    selectedHash: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    branch: String? = null,
    folder: String? = null,
) {
    val strings = historyStrings()
    Column(modifier.ckSurface(MoruSurfaceRole.Page)) {
        GitWipRow(files, selected = selectedHash == null, onClick = { onSelect(null) }, branch = branch, folder = folder)
        when (history) {
            null -> Message(strings.loading)
            is GitHistoryResult.Unavailable -> Message(history.message)
            is GitHistoryResult.Loaded -> if (history.commits.isEmpty()) {
                Message(strings.noCommits)
            } else {
                val rows: List<GitGraphRow> = remember(history) { layoutGitGraph(history.commits) }
                GitHistoryList(
                    rows = rows,
                    selectedHash = selectedHash,
                    nowEpochSeconds = remember(history) { Clock.System.now().epochSeconds },
                    onSelect = { hash -> onSelect(hash) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        MoruText(text, color = MoruTheme.colors.textSecondary)
    }
}
