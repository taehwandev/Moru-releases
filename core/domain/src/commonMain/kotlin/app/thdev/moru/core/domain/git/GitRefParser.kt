package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitWorktreeInfo
import app.thdev.moru.core.model.git.history.GitNamedRef
import app.thdev.moru.core.model.git.history.GitRef

/** Pure interpretation of complete ref and NUL worktree transports; callers strictly decode UTF-8. */
object GitRefParser {
    fun worktrees(text: String): Map<String, String> = worktreeList(text).mapNotNull {
        it.branch?.let { branch -> branch to it.path }
    }.toMap()

    fun worktreeList(text: String): List<GitWorktreeInfo> {
        if (text.isEmpty()) return emptyList()
        require(text.endsWith("\u0000\u0000")) { "Git returned incomplete worktrees." }
        val result = mutableListOf<GitWorktreeInfo>()
        var current: GitWorktreeInfo? = null
        var hasHead = false
        for (record in text.dropLast(1).split('\u0000')) {
            when {
                record.startsWith("worktree ") -> {
                    require(current == null) { "Git returned an incomplete worktree record." }
                    val path = record.removePrefix("worktree ")
                    require(path.startsWith('/') && path.length > 1) { "Git returned an invalid worktree path." }
                    current = GitWorktreeInfo(path, null, null, result.isEmpty())
                    hasHead = false
                }
                record.isEmpty() -> {
                    val entry = requireNotNull(current) { "Git returned an empty worktree record." }
                    require(hasHead) { "Git returned a worktree without HEAD." }
                    result += entry
                    current = null
                }
                else -> {
                    val entry = requireNotNull(current) { "Git returned malformed worktrees." }
                    when {
                        record.startsWith("HEAD ") -> {
                            val head = record.removePrefix("HEAD ")
                            require(!hasHead && GitLogRecords.isObjectID(head)) { "Git returned invalid worktree HEAD." }
                            hasHead = true
                            current = entry.copy(head = head.takeUnless { value -> value.all { it == '0' } })
                        }
                        record.startsWith("branch ") -> {
                            val ref = record.removePrefix("branch ")
                            require(validPublicRef(ref) && ref.startsWith("refs/heads/")) { "Git returned invalid worktree branch." }
                            current = entry.copy(branch = ref.removePrefix("refs/heads/"))
                        }
                        record == "locked" || record.startsWith("locked ") -> current = entry.copy(locked = true)
                        record == "prunable" || record.startsWith("prunable ") -> current = entry.copy(prunable = true)
                        record == "detached" -> Unit
                        record == "bare" -> hasHead = true
                        else -> throw IllegalArgumentException("Git returned an unknown worktree field.")
                    }
                }
            }
        }
        require(current == null && result.map { it.path }.toSet().size == result.size) {
            "Git returned incomplete or duplicate worktrees."
        }
        return result
    }

    fun tags(text: String): List<String> = completeLines(text).map { it.substringBefore('\u0000') }
        .filter { it.startsWith("refs/tags/") }.map { it.removePrefix("refs/tags/") }

    fun branches(text: String, merged: Set<String>, worktrees: Map<String, String>): List<GitBranchInfo> =
        completeLines(text).map { line ->
            val parts = line.split('\u0000')
            require(parts.size == 5 && parts.last().isEmpty() && validPublicRef(parts[0])) {
                "Git returned malformed branches."
            }
            val remote = parts[0].startsWith("refs/remotes/")
            require(remote || parts[0].startsWith("refs/heads/")) { "Git returned unsupported branch." }
            val name = parts[0].removePrefix(if (remote) "refs/remotes/" else "refs/heads/")
            GitBranchInfo(name, parts[3] == "*", remote, parts[2].ifEmpty { null }?.removePrefix("refs/remotes/"),
                if (remote) null else worktrees[name], parts[0] in merged)
        }

    fun namedRefs(text: String, worktrees: Map<String, String>): List<GitNamedRef> = completeLines(text).map { line ->
        val parts = line.split('\u0000')
        require(parts.size == 6 && parts.last().isEmpty() && validPublicRef(parts[0]) &&
            GitLogRecords.isObjectID(parts[1]) && parts[3] in setOf("*", " ") &&
            (parts[2].isEmpty() || validPublicRef(parts[2])) &&
            (parts[4].isEmpty() || GitLogRecords.isObjectID(parts[4]))) { "Git returned malformed refs." }
        val kind = when {
            parts[0].startsWith("refs/heads/") -> GitRef.Kind.LocalBranch
            parts[0].startsWith("refs/remotes/") -> GitRef.Kind.RemoteBranch
            else -> GitRef.Kind.Tag
        }
        val name = parts[0].split('/').drop(2).joinToString("/")
        GitNamedRef(parts[0], name, kind, parts[4].ifEmpty { parts[1] }, parts[3] == "*",
            parts[2].ifEmpty { null }, if (kind == GitRef.Kind.LocalBranch) worktrees[name] else null)
    }.also { refs -> require(refs.map { it.fullName }.toSet().size == refs.size) { "Git returned duplicate refs." } }

    fun validPublicRef(ref: String): Boolean = listOf("refs/heads/", "refs/remotes/", "refs/tags/").any {
        ref.startsWith(it) && ref.length > it.length
    } && ref.none { it <= ' ' || it == '\u007f' || it in "~^:?*[\\" } &&
        !ref.contains("..") && !ref.contains("@{") && !ref.endsWith('.') &&
        ref.split('/').none { it.isEmpty() || it.startsWith('.') || it.endsWith(".lock") }

    private fun completeLines(text: String): List<String> {
        require(text.isEmpty() || text.endsWith('\n')) { "Git returned incomplete refs." }
        return if (text.isEmpty()) emptyList() else text.dropLast(1).split('\n').also {
            require(it.none { line -> line.isEmpty() || '\r' in line }) { "Git returned malformed refs." }
        }
    }
}
