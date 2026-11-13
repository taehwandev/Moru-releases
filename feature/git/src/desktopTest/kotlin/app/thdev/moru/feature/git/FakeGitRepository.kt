package app.thdev.moru.feature.git

import app.thdev.moru.core.domain.GitRepository
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState
import java.util.concurrent.CopyOnWriteArrayList

internal class FakeGitRepository : GitRepository {
    val reads = CopyOnWriteArrayList<String>()
    val calls = CopyOnWriteArrayList<GitCall>()
    @Volatile var readState: GitRepositoryState = GitRepositoryState.Loaded(GitManagementFixtures.snapshot)
    @Volatile var result: GitActionResult = GitActionResult.Succeeded("Action completed")
    var reader: ((String) -> GitRepositoryState)? = null
    var beforeRead: (() -> Unit)? = null
    var onExecute: ((GitCall) -> GitActionResult)? = null

    override fun snapshot(workspacePath: String): GitRepositoryState {
        reads += workspacePath
        beforeRead?.invoke()
        return reader?.invoke(workspacePath) ?: readState
    }

    override fun execute(workspacePath: String, expectedRevision: String, action: GitAction): GitActionResult {
        val call = GitCall(workspacePath, expectedRevision, action)
        calls += call
        return onExecute?.invoke(call) ?: result
    }
}

internal data class GitCall(val folder: String, val revision: String, val action: GitAction)
