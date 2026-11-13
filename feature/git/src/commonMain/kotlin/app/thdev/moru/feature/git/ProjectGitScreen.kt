package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.thdev.moru.core.domain.GitRepository
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.feature.git.history.GitHistoryScreen
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * The Project's Git tab: the whole repository on one wide screen, for the explicitly selected Project/Task execution
 * folder. Without a repository to manage it falls back to the read-only history.
 */
@Composable
fun ProjectGitScreen(
    workspacePath: String,
    loadGitHistory: (workspacePath: String) -> GitHistoryResult,
    loadCommitDiff: (workspacePath: String, hash: String) -> CommitDiffResult,
    modifier: Modifier = Modifier,
    gitRepository: GitRepository? = null,
    onRepositoryChanged: () -> Unit = {},
    loadWorkingChanges: (workspacePath: String) -> CommitDiffResult = {
        CommitDiffResult.Unavailable("Working changes are unavailable.")
    },
    detailWidth: Dp? = null,
    onDetailWidthChange: (Dp?) -> Unit = {},
    onOpenWorktree: (path: String) -> Unit = {},
) {
    if (gitRepository != null) {
        key(workspacePath) {
            GitManagementScreen(
                workspacePath = workspacePath,
                repository = gitRepository,
                history = GitHistorySource(loadGitHistory, loadCommitDiff, loadWorkingChanges),
                onRepositoryChanged = onRepositoryChanged,
                detailWidth = detailWidth,
                onDetailWidthChange = onDetailWidthChange,
                modifier = modifier,
                onOpenWorktree = onOpenWorktree,
            )
        }
    } else ReadOnlyHistory(workspacePath, loadGitHistory, loadCommitDiff, modifier)
}

@Composable
private fun ReadOnlyHistory(
    workspacePath: String,
    loadGitHistory: (String) -> GitHistoryResult,
    loadCommitDiff: (String, String) -> CommitDiffResult,
    modifier: Modifier = Modifier,
) {
    var refreshKey by remember { mutableStateOf(0) }
    val history by produceState<GitHistoryResult?>(null, workspacePath, refreshKey) {
        value = null
        value = withContext(Dispatchers.Default) { loadGitHistory(workspacePath) }
    }
    GitHistoryScreen(
        result = history,
        nowEpochSeconds = remember(history) { Clock.System.now().epochSeconds },
        onRefresh = { refreshKey += 1 },
        loadCommitDiff = { hash -> loadCommitDiff(workspacePath, hash) },
        modifier = modifier,
    )
}
