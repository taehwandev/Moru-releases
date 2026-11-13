package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.git.diff.CommitDiffResult
/** What the Code panel reads from the desktop. Every call may start a process, so the panel calls it off the UI thread. */
interface CodePanelSource {
    /** The checkouts of the repository [projectFolder] is in, main first; empty when it is not in git. */
    fun worktrees(projectFolder: String): List<CodeWorktree>

    /** The checkout roots the programs of terminal session [sessionId] work in right now. */
    fun sessionWorktreeRoots(sessionId: String): List<String>

    /** A value that changes whenever the changes in [worktreeRoot] do, cheaper to read than the changes; null when unknown. */
    fun changeStamp(worktreeRoot: String): String?

    /** The changes in [worktreeRoot] since its last commit, including files git does not track yet. */
    fun changes(worktreeRoot: String): CommitDiffResult

    /** The file at [relativePath] in [worktreeRoot] as it is now. */
    fun readFile(worktreeRoot: String, relativePath: String): WorkbenchFilePreview
}
