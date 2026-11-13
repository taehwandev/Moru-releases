package app.thdev.moru.core.data.git

import app.thdev.moru.core.domain.GitRepository
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositoryState

class DesktopGitRepository internal constructor(private val runner: GitProcessRunner) : GitRepository {
    constructor() : this(GitProcessRunner())

    override fun snapshot(workspacePath: String): GitRepositoryState = try {
        val context = GitRepositoryDiscovery.read(workspacePath, runner)
        GitRepositoryLocks.withRepository(context) {
            GitRepositoryState.Loaded(GitSnapshotReader(runner).read(context))
        }
    } catch (error: Exception) {
        GitRepositoryState.Unavailable(diagnostic(error, "The selected folder is not an available Git repository."))
    }

    override fun execute(workspacePath: String, expectedRevision: String, action: GitAction): GitActionResult = try {
        val context = GitRepositoryDiscovery.read(workspacePath, runner)
        GitRepositoryLocks.withRepository(context) {
            val snapshot = GitSnapshotReader(runner).read(context)
            if (snapshot.revision != expectedRevision) {
                GitActionResult.Rejected("The repository changed after this proposal. Refresh and confirm the action again.")
            } else {
                val rejection = GitActionPolicy(runner).rejection(context, snapshot, action)
                if (rejection != null) GitActionResult.Rejected(rejection)
                else GitActionExecutor(runner).execute(context, snapshot, action)
            }
        }
    } catch (error: Exception) {
        GitActionResult.Failed(diagnostic(error, "Git could not complete this action. Refresh the repository before continuing."))
    }

    private fun diagnostic(error: Exception, fallback: String): String =
        GitDiagnosticRedactor.redact(error.message?.takeIf { it.isNotBlank() } ?: fallback)
}
