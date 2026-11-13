package app.thdev.moru.feature.git

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.feature.git.components.branches.GitBranchCreateForm
import app.thdev.moru.feature.git.components.branches.GitBranchRenameForm

/**
 * The name form a ref menu opened: a new branch from HEAD or from the ref, or a new name for a local branch. Proposing
 * closes it; the change itself still waits for approval. [GitBranchEditor.Closed] shows nothing.
 */
@Composable
internal fun GitBranchActions(
    state: GitManagementUiState,
    editor: GitBranchEditor,
    onClose: () -> Unit,
    onEvent: (GitManagementEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = state.snapshot ?: return
    val available = state.interactive && snapshot.operationInProgress == null
    fun propose(action: GitAction) {
        onClose()
        onEvent(GitManagementEvent.Propose(action))
    }
    Box(modifier) {
        when (editor) {
            GitBranchEditor.Closed -> Unit
            is GitBranchEditor.Create -> GitBranchCreateForm(state.newBranchName, { onEvent(GitManagementEvent.EditNewBranchName(it)) },
                editor.source?.name, available, snapshot.head != null,
                { propose(GitAction.CreateBranch(state.newBranchName.trim(), checkout = false)) },
                { editor.source?.let { propose(GitAction.CreateBranch(state.newBranchName.trim(), it.actionRef, checkout = false)) } },
                onClose)
            is GitBranchEditor.Rename -> {
                val occupied = editor.branch.checkedOutPath != null && editor.branch.checkedOutPath != snapshot.rootPath
                GitBranchRenameForm(editor.branch.name, state.renameBranchName, { onEvent(GitManagementEvent.EditRenameBranchName(it)) },
                    available && !occupied,
                    state.renameBranchName.isNotBlank() && state.renameBranchName.trim() != editor.branch.name,
                    { propose(GitAction.RenameBranch(editor.branch.name, state.renameBranchName.trim())) }, onClose)
            }
        }
    }
}
