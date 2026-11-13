package app.thdev.moru.app

import app.thdev.moru.core.data.agent.AgentWorkFolders
import app.thdev.moru.core.domain.ProjectFileRepository
import app.thdev.moru.core.domain.WorkbenchGitReader
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.terminal.SessionProcessFiles
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.feature.codepanel.CodePanelSource

/** The Code panel's reads on the desktop, through the same git reader and file reader the Git and Code views use. */
internal class DesktopCodePanelSource(
    private val git: WorkbenchGitReader,
    private val files: ProjectFileRepository,
    private val agents: AgentWorkFolders = AgentWorkFolders(),
) : CodePanelSource {
    override fun worktrees(projectFolder: String): List<CodeWorktree> = git.worktrees(projectFolder)

    // What the agents' records name comes first: it stays after their commands end, while a command's folder shows only
    // while it runs.
    override fun sessionWorktreeRoots(sessionId: String): List<String> {
        val tree = SessionProcessFiles.read(sessionId) ?: return emptyList()
        val folders = agents.read(tree.pids, tree.files.openFiles) + tree.files.workingFolders
        return folders.distinct().mapNotNull(git::repositoryRoot).distinct()
    }

    override fun changeStamp(worktreeRoot: String): String? = git.changeStamp(worktreeRoot)

    override fun changes(worktreeRoot: String): CommitDiffResult = git.workingChanges(worktreeRoot)

    override fun readFile(worktreeRoot: String, relativePath: String): WorkbenchFilePreview =
        files.open(worktreeRoot, relativePath)
}
