package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.domain.WorkbenchGitReader
import app.thdev.moru.core.model.git.ChangedFileState
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.history.GitHistoryResult
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** The Workbench's git reads on the desktop, through the git CLI. */
class DesktopWorkbenchGitReader : WorkbenchGitReader {
    // A folder stays in the same checkout while it exists, so each one costs a git run once, not every reading.
    private val rootsByFolder = ConcurrentHashMap<String, String>()

    override fun status(projectRoot: String): WorkbenchGitStatus = WorkbenchGitStatusScanner.scan(File(projectRoot))

    override fun history(projectRoot: String): GitHistoryResult = WorkbenchGitLogScanner.scan(File(projectRoot))

    override fun commitChanges(projectRoot: String, hash: String): CommitDiffResult =
        WorkbenchGitDiffScanner.commit(File(projectRoot), hash)

    override fun workingChanges(projectRoot: String): CommitDiffResult =
        WorkbenchGitDiffScanner.workingTree(File(projectRoot))

    override fun worktrees(projectFolder: String): List<CodeWorktree> {
        val run = WorkbenchGitProcess.run(File(projectFolder), "worktree", "list", "--porcelain") ?: return emptyList()
        if (run.exitCode != 0) return emptyList()
        return parseWorktreeList(run.stdout).map { worktree -> worktree.copy(path = canonical(worktree.path)) }
    }

    override fun changeStamp(worktreeRoot: String): String? {
        val (status, files) = scanChanges(worktreeRoot) ?: return null
        // Editing a file that is already changed leaves the status as it was, so each listed file's size and time count too.
        return status + files.values.joinToString("") { file -> "|${file.stamp}" }
    }

    override fun changedFiles(worktreeRoot: String): Map<String, ChangedFileState>? = scanChanges(worktreeRoot)?.second

    private fun scanChanges(worktreeRoot: String): Pair<String, Map<String, ChangedFileState>>? {
        val root = File(worktreeRoot)
        val run = WorkbenchGitProcess.run(root, "status", "--porcelain=v1", "-z", "--untracked-files=all") ?: return null
        if (run.exitCode != 0 || run.truncated) return null
        // Each entry is "XY path": X the index side, Y the working file side ('?' for a file git does not track yet).
        val entries = run.stdout.split('\u0000').filter { entry -> entry.length > 3 }
        return run.stdout to entries.associate { entry ->
            val path = entry.substring(3)
            path to ChangedFileState(
                stamp = File(root, path).let { "${it.length()}:${it.lastModified()}" },
                unstaged = entry[1] != ' ',
            )
        }
    }

    override fun repositoryRoot(folder: String): String? {
        if (!File(folder).isDirectory) {
            rootsByFolder.remove(folder)
            return null
        }
        rootsByFolder[folder]?.let { root -> return root }
        val run = WorkbenchGitProcess.run(File(folder), "rev-parse", "--show-toplevel") ?: return null
        if (run.exitCode != 0) return null
        return run.stdout.trim().takeIf(String::isNotEmpty)?.let(::canonical)?.also { root -> rootsByFolder[folder] = root }
    }

    // git prints a checkout as it was named and lsof as the disk resolves it, so both are compared resolved.
    private fun canonical(path: String): String = runCatching { File(path).canonicalPath }.getOrDefault(path)
}
