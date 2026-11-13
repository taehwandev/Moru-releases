package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.thdev.moru.core.domain.GitRepository

/**
 * The Git screen for one folder. [detailWidth] is the detail pane width the user last dragged to (null follows the
 * window); each change is reported through [onDetailWidthChange] so it can be kept beyond this screen.
 */
@Composable
fun GitManagementScreen(
    workspacePath: String,
    repository: GitRepository?,
    history: GitHistorySource,
    onRepositoryChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
    detailWidth: Dp? = null,
    onDetailWidthChange: (Dp?) -> Unit = {},
    /** A worktree's branch was picked; the owner moves Git to that worktree's folder. */
    onOpenWorktree: (path: String) -> Unit = {},
) {
    key(workspacePath, repository) {
        val scope = rememberCoroutineScope()
        val notifyChanged by rememberUpdatedState(onRepositoryChanged)
        val holder = remember(workspacePath, repository, scope) {
            GitManagementStateHolder(workspacePath, repository, scope) { notifyChanged() }
        }
        var width by remember { mutableStateOf(detailWidth) }
        LaunchedEffect(holder) { holder.load() }
        val onEvent: (GitManagementEvent) -> Unit = { event -> if (event is GitManagementEvent.OpenWorktree) onOpenWorktree(event.path) else holder.onEvent(event) }
        GitManagementContent(
            holder.state, workspacePath,
            workspaceBody = { bodyModifier ->
                GitWorkspaceBody(
                    holder.state, workspacePath, history, onEvent, bodyModifier,
                    detailWidth = width, onDetailWidthChange = { width = it; onDetailWidthChange(it) },
                )
            },
            modifier = modifier,
            onEvent = onEvent,
        )
    }
}
