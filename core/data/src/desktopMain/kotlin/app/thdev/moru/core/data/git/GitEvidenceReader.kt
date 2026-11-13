package app.thdev.moru.core.data.git

internal class GitEvidenceReader(private val runner: GitProcessRunner) {
    fun read(context: GitRepositoryDiscovery): GitReadEvidence {
        val values = linkedMapOf<String, ByteArray>()
        fun read(key: String, vararg args: String, optional: Boolean = false) {
            val result = runner.run(context.root, args.toList())
            if (optional && result.exitCode == 1 && !result.uncertain) values[key] = ByteArray(0)
            else values[key] = result.requireSuccess()
        }
        read("status", "status", "--porcelain=v1", "-z", "--untracked-files=all", "--ignore-submodules=none")
        read("head", "rev-parse", "--verify", "--quiet", "HEAD", optional = true)
        read("branch", "symbolic-ref", "--quiet", "HEAD", optional = true)
        read("refs", "for-each-ref", "--format=%(refname)%00%(objectname)%00%(upstream)%00%(HEAD)%00", "refs/heads", "refs/remotes")
        read("allRefs", "for-each-ref", "--format=%(refname)%00%(objectname)%00%(symref)%00")
        read("worktrees", "worktree", "list", "--porcelain", "-z")
        read("remotes", "remote")
        read("config", "config", "--null", "--list", "--show-origin")
        read("index", "ls-files", "--stage", "-z")
        read("files", "ls-files", "--cached", "--others", "--exclude-standard", "-z")
        read("ssh", "config", "--get", "core.sshCommand", optional = true)
        val symbolic = GitStatusParser.decode(values.getValue("branch")).removeSuffix("\n")
        require(symbolic.isEmpty() || symbolic.startsWith("refs/heads/")) { "Git HEAD points to an unsupported symbolic ref." }
        val branch = symbolic.removePrefix("refs/heads/")
        if (branch.isNotEmpty()) {
            read("upstreamRemote", "config", "--get", "branch.$branch.remote", optional = true)
            read("upstreamBranch", "config", "--get", "branch.$branch.merge", optional = true)
        } else {
            values["upstreamRemote"] = ByteArray(0)
            values["upstreamBranch"] = ByteArray(0)
        }
        if (values.getValue("head").isNotEmpty()) {
            read("merged", "for-each-ref", "--merged=HEAD", "--format=%(refname)", "refs/heads", "refs/remotes")
        } else {
            require(values.getValue("branch").isNotEmpty()) { "Git HEAD is invalid." }
            values["merged"] = ByteArray(0)
        }
        return GitReadEvidence(values)
    }
}
