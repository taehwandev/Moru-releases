package app.thdev.moru.core.data.git

import app.thdev.moru.core.model.git.GitRepositorySnapshot

internal class GitSnapshotReader(private val runner: GitProcessRunner) {
    fun read(context: GitRepositoryDiscovery): GitRepositorySnapshot {
        val reader = GitEvidenceReader(runner)
        val evidence = reader.read(context)
        val (revision, operation) = GitRevisionFingerprint().read(context, evidence)
        val occupancy = GitRefParser.worktrees(evidence.bytes("worktrees"))
        val merged = evidence.text("merged").lineSequence().filter { it.isNotEmpty() }.toSet()
        val branches = GitRefParser.branches(evidence.bytes("refs"), merged, occupancy)
        val branch = evidence.text("branch").removePrefix("refs/heads/").ifEmpty { null }
        val upstream = branches.singleOrNull { it.name == branch && !it.isRemote }?.upstream
        val counts = if (upstream != null && evidence.bytes("head").isNotEmpty()) {
            val ref = if (upstream.startsWith("refs/")) upstream else "refs/remotes/$upstream"
            val exists = runner.run(context.root, listOf("show-ref", "--verify", "--quiet", ref))
            check(!exists.uncertain && exists.exitCode in 0..1) { "Git could not resolve the upstream." }
            if (exists.exitCode == 0) {
                val count = runner.run(context.root, listOf("rev-list", "--left-right", "--count", "HEAD...$ref"))
                count.requireSuccess()
                val parts = count.text().split(Regex("\\s+"))
                require(parts.size == 2) { "Git returned malformed synchronization counts." }
                parts.map { it.toInt() }
            } else null
        } else null
        check(evidence.sameAs(reader.read(context))) { "The repository changed while loading. Refresh to read it again." }
        val remotes = evidence.text("remotes").lineSequence().filter { it.isNotEmpty() }.toList()
        require(remotes.all { safeName(it) }) { "Git returned unsupported remote names." }
        return GitRepositorySnapshot(context.root.toString(), branch, evidence.text("head").ifEmpty { null },
            GitStatusParser.parse(evidence.bytes("status")), branches, remotes, revision, upstream,
            evidence.text("upstreamRemote").ifEmpty { null },
            evidence.text("upstreamBranch").ifEmpty { null }?.removePrefix("refs/heads/"),
            counts?.get(0), counts?.get(1), operation,
            tags = GitRefParser.tags(evidence.bytes("allRefs")),
            worktrees = GitRefParser.worktreeList(evidence.bytes("worktrees")))
    }

    private fun safeName(value: String): Boolean = !value.startsWith('-') && value.none { it.isISOControl() || it.isWhitespace() }
}
