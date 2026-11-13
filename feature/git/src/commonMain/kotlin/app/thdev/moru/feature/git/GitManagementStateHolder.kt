package app.thdev.moru.feature.git

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.GitRepository
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Folder-scoped presentation state; the screen's scope cancels all jobs when that folder leaves composition. */
internal class GitManagementStateHolder(
    private val workspacePath: String,
    private val repository: GitRepository?,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val notifyChanged: () -> Unit,
) {
    var state by mutableStateOf(GitManagementUiState())
        private set

    fun onEvent(event: GitManagementEvent) {
        if (!state.idle) return
        when (event) {
            GitManagementEvent.Refresh -> {
                state = state.copy(loading = true, approval = null, feedback = null)
                scope.launch { load(); notifyChanged() }
            }
            GitManagementEvent.Confirm -> state.approval?.let { execute(it.action, it.revision) }
            is GitManagementEvent.Propose -> if (state.interactive) {
                val action = event.action
                if (action is GitAction.Fetch || action is GitAction.Pull || action is GitAction.Push) {
                    state.snapshot?.let { execute(action, it.revision) }
                } else state = state.edit(event, workspacePath)
            }
            is GitManagementEvent.ChangeIndex -> {
                if (state.interactive && (event.action is GitAction.Stage || event.action is GitAction.Unstage)) {
                    execute(event.action, event.revision)
                }
            }
            GitManagementEvent.Cancel -> state = state.edit(event, workspacePath)
            else -> if (state.interactive) state = state.edit(event, workspacePath)
        }
    }

    suspend fun load() {
        state = state.copy(loading = true, approval = null)
        val result = withContext(io) {
            when {
                workspacePath.isBlank() -> GitRepositoryState.Unavailable("No folder is selected. Select a Project or Task folder.")
                repository == null -> GitRepositoryState.Unavailable("Git management is unavailable for this folder.")
                else -> try {
                    repository.snapshot(workspacePath)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    GitRepositoryState.Unavailable("Git repository read failed. Refresh to try again.")
                }
            }
        }
        state = state.afterRead(result)
    }

    private fun execute(action: GitAction, revision: String) {
        val port = repository ?: return
        if (!state.idle) return
        state = state.copy(busy = true, approval = null, feedback = null)
        scope.launch {
            val result = withContext(io) {
                try {
                    port.execute(workspacePath, revision, action)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    GitActionResult.Failed("Git action failed. Review the refreshed repository before retrying.")
                }
            }
            state = state.copy(
                feedback = when (result) {
                    is GitActionResult.Succeeded -> "Succeeded: ${result.message}"
                    is GitActionResult.Rejected -> "Rejected: ${result.message}"
                    is GitActionResult.Failed -> "Failed: ${result.message}"
                },
                feedbackIsFailure = result !is GitActionResult.Succeeded,
                commitMessage = if (action is GitAction.Commit && result is GitActionResult.Succeeded) "" else state.commitMessage,
            )
            load()
            notifyChanged()
        }
    }
}
