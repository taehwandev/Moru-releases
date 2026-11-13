package app.thdev.moru.core.data.git.workbench

import app.thdev.moru.core.model.git.history.GitRef
import java.io.File

/**
 * The other checkouts of [projectRoot]'s repository: where each has its HEAD and how many files changed there are not
 * committed yet. A detached worktree's HEAD is under no branch, so `git log` only walks it when it is named. The
 * checkout being shown is left out; its HEAD and changes have their own rows.
 */
internal object WorkbenchGitWorktreeHeads {
    class Head(val hash: String, val ref: GitRef)

    fun scan(projectRoot: File): List<Head> {
        val list = WorkbenchGitProcess.run(projectRoot, "worktree", "list", "--porcelain") ?: return emptyList()
        if (list.exitCode != 0) return emptyList()
        val heads = parseWorktreeHeads(list.stdout)
        if (heads.size < 2) return emptyList()
        val top = WorkbenchGitProcess.run(projectRoot, "rev-parse", "--show-toplevel")
            ?.stdout?.trim()?.let(::canonical)
        return heads.mapNotNull { (path, hash) ->
            val folder = File(path)
            if (canonical(path) == top || !folder.isDirectory) return@mapNotNull null
            Head(hash, GitRef(folder.name, GitRef.Kind.Worktree, pendingChanges = pendingChanges(folder)))
        }
    }

    private fun pendingChanges(folder: File): Int {
        val run = WorkbenchGitProcess.run(folder, "status", "--porcelain=v1", "-z", maxChars = MaxStatusChars) ?: return 0
        if (run.exitCode != 0) return 0
        // A rename or copy is followed by its original path as an entry of its own, without a status prefix.
        val entries = run.stdout.split('\u0000').filter { entry -> entry.isNotEmpty() }.iterator()
        var count = 0
        while (entries.hasNext()) {
            val entry = entries.next()
            count += 1
            if (entry.first() == 'R' || entry.first() == 'C') if (entries.hasNext()) entries.next()
        }
        return count
    }

    private fun canonical(path: String): String = runCatching { File(path).canonicalPath }.getOrDefault(path)

    private const val MaxStatusChars = 200_000
}
