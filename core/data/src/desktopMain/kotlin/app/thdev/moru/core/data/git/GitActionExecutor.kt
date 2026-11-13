package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitActionResult
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS

internal class GitActionExecutor(private val runner: GitProcessRunner) {
    fun execute(context: GitRepositoryDiscovery, snapshot: GitRepositorySnapshot, action: GitAction): GitActionResult {
        if (action is GitAction.Discard) return discard(context, snapshot, action.paths)
        val policy = GitActionPolicy(runner)
        val args = when (action) {
            is GitAction.Discard -> error("handled above")
            is GitAction.Stage -> listOf("add", "--all", "--") + stagePaths(context, snapshot, action.paths)
            is GitAction.Unstage -> if (snapshot.head == null) {
                listOf("rm", "--cached", "--force", "--ignore-unmatch", "--") + paths(snapshot, action.paths)
            } else listOf("restore", "--staged", "--source=HEAD", "--") + paths(snapshot, action.paths)
            is GitAction.Commit -> listOf("commit", "--message", action.message)
            is GitAction.Fetch -> listOf("fetch", "--", action.remote)
            is GitAction.Pull -> listOf("pull", "--ff-only", "--no-rebase", "--no-autostash", "--", action.remote, "refs/heads/${action.branch}")
            is GitAction.Push -> listOf("-c", "remote.${action.remote}.mirror=false", "push", "--no-force", "--no-follow-tags", "--recurse-submodules=no") +
                (if (action.setUpstream) listOf("--set-upstream") else emptyList()) +
                listOf("--", action.remote, "refs/heads/${snapshot.branchName}:refs/heads/${action.branch}")
            is GitAction.CreateBranch -> if (action.checkout) {
                listOf("switch", "--no-guess", "--create", action.name, policy.target(snapshot, action.startPoint ?: snapshot.branchName.orEmpty()) ?: snapshot.head!!)
            } else listOf("branch", "--", action.name, action.startPoint?.let { policy.target(snapshot, it) } ?: snapshot.head!!)
            is GitAction.SwitchBranch -> listOf("switch", "--no-guess", "--", action.name)
            is GitAction.RenameBranch -> listOf("branch", "--move", "--", action.oldName, action.newName)
            is GitAction.DeleteBranch -> listOf("branch", "--delete") + (if (action.force) listOf("--force") else emptyList()) +
                listOf("--", action.name)
            is GitAction.DeleteTag -> listOf("tag", "--delete", "--", action.name)
            // Without --force Git refuses a worktree with modified or untracked files.
            is GitAction.RemoveWorktree -> listOf("worktree", "remove", "--", action.path)
            GitAction.PruneWorktrees -> listOf("worktree", "prune")
            is GitAction.MergeBranch -> listOf("merge", "--ff-only", "--no-edit", "--", policy.target(snapshot, action.name)!!)
        }
        val network = action is GitAction.Fetch || action is GitAction.Pull || action is GitAction.Push
        val ssh = if (network) runner.run(context.root, listOf("config", "--get", "core.sshCommand")) else null
        if (ssh != null && (ssh.uncertain || ssh.exitCode !in 0..1)) return GitActionResult.Failed("Git could not read SSH configuration.")
        val result = runner.run(context.root, args, batchSsh = network && ssh?.exitCode == 1)
        return if (result.exitCode == 0 && !result.uncertain) GitActionResult.Succeeded("Git action completed. Refresh to review the repository.")
        else GitActionResult.Failed(if (result.uncertain) result.stderr else
            GitDiagnosticRedactor.redact(result.stderr + "\n" + result.text()).trim().ifBlank {
                "Git rejected this action. Refresh and review the repository before continuing."
            })
    }

    /** Files the last commit has are restored from it, index and working copy; files it does not have are removed. */
    private fun discard(context: GitRepositoryDiscovery, snapshot: GitRepositorySnapshot, selected: List<String>): GitActionResult {
        val chosen = paths(snapshot, selected)
        val committed = runner.run(context.root, listOf("ls-tree", "-r", "-z", "--name-only", "HEAD", "--") + chosen)
            .requireSuccess().let(GitStatusParser::records).toSet()
        val steps = listOfNotNull(
            chosen.filter { it in committed }.takeIf { it.isNotEmpty() }
                ?.let { listOf("restore", "--source=HEAD", "--staged", "--worktree", "--") + it },
            chosen.filter { it !in committed }.takeIf { it.isNotEmpty() }
                ?.let { listOf("rm", "--cached", "--force", "--ignore-unmatch", "--quiet", "--") + it },
        )
        for (args in steps) {
            val result = runner.run(context.root, args)
            if (result.exitCode != 0 || result.uncertain) return GitActionResult.Failed(
                GitDiagnosticRedactor.redact(result.stderr).trim().ifBlank { "Git could not discard these changes." },
            )
        }
        // New files are not in the last commit; discarding them means deleting the working copy too.
        chosen.filter { it !in committed }.forEach { path -> Files.deleteIfExists(context.root.resolve(path)) }
        return GitActionResult.Succeeded("Changes discarded. Refresh to review the repository.")
    }

    private fun paths(snapshot: GitRepositorySnapshot, selected: List<String>): List<String> = selected.flatMap { path ->
        val file = snapshot.files.single { it.path == path }
        listOfNotNull(file.path, file.originalPath)
    }.distinct()

    private fun stagePaths(context: GitRepositoryDiscovery, snapshot: GitRepositorySnapshot, selected: List<String>): List<String> {
        val tracked = runner.run(context.root, listOf("ls-files", "-z")).requireSuccess().let(GitStatusParser::records).toSet()
        return paths(snapshot, selected).filter { it in tracked || Files.exists(context.root.resolve(it), NOFOLLOW_LINKS) }
            .also { require(it.isNotEmpty()) { "No stageable paths remain. Refresh the repository." } }
    }
}
