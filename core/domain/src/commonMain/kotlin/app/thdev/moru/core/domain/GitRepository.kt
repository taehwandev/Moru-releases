package app.thdev.moru.core.domain

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState

/** Call off the UI thread. Execute only the action explicitly approved for this revision. */
interface GitRepository {
    fun snapshot(workspacePath: String): GitRepositoryState
    fun execute(workspacePath: String, expectedRevision: String, action: GitAction): GitActionResult
}
