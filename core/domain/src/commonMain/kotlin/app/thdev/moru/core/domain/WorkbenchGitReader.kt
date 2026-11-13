package app.thdev.moru.core.domain

import app.thdev.moru.core.model.git.ChangedFileState
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.history.GitHistoryResult

/**
 * What the Workbench reads from git: status, history, changes and checkouts. Every call may start a
 * process, so callers run it off the UI thread. Paths are absolute folder paths.
 */
interface WorkbenchGitReader {
    fun status(projectRoot: String): WorkbenchGitStatus

    fun history(projectRoot: String): GitHistoryResult

    /** What one commit changed; a merge against its first parent. */
    fun commitChanges(projectRoot: String, hash: String): CommitDiffResult

    /** Changes since the last commit, including files git does not track yet. */
    fun workingChanges(projectRoot: String): CommitDiffResult

    /** The checkouts of the repository [projectFolder] is in, main first; empty when it is not in git. */
    fun worktrees(projectFolder: String): List<CodeWorktree>

    /** A value that changes whenever the changes in [worktreeRoot] do; null when unknown. */
    fun changeStamp(worktreeRoot: String): String?

    /** Each changed or new file in [worktreeRoot] (path from that root) and its state; null when unknown. */
    fun changedFiles(worktreeRoot: String): Map<String, ChangedFileState>? = null

    /** The top folder of the checkout [folder] is in, resolved on disk; null outside git. */
    fun repositoryRoot(folder: String): String?
}
