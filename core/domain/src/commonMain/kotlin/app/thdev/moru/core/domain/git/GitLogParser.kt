package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.history.GitCommit

import app.thdev.moru.core.model.git.history.GitRef
/**
 * The `--pretty` format [parseGitLog] understands. Fields are separated by the ASCII unit
 * separator and records by the record separator, so commit messages cannot break parsing.
 * Run `git log --topo-order --decorate=full` with it so refs arrive fully qualified.
 */
const val GIT_LOG_PRETTY_FORMAT = "format:%H%x1f%P%x1f%an%x1f%ae%x1f%at%x1f%D%x1f%s%x1e"

private const val FIELD_SEPARATOR = '\u001f'
private const val RECORD_SEPARATOR = '\u001e'
private const val FIELD_COUNT = 7

/**
 * Parse [output] produced with [GIT_LOG_PRETTY_FORMAT]. Records that do not have all
 * seven fields are skipped instead of failing the whole history.
 */
fun parseGitLog(output: String): List<GitCommit> =
    output.split(RECORD_SEPARATOR)
        .map { record -> record.trim('\n', '\r') }
        .filter { record -> record.isNotEmpty() }
        .mapNotNull { record -> parseRecord(record) }

private fun parseRecord(record: String): GitCommit? {
    val fields = record.split(FIELD_SEPARATOR)
    if (fields.size < FIELD_COUNT) return null
    val hash = fields[0].trim()
    if (hash.isEmpty()) return null
    return GitCommit(
        hash = hash,
        parents = fields[1].trim().split(' ').filter { parent -> parent.isNotEmpty() },
        authorName = fields[2],
        authorEmail = fields[3],
        epochSeconds = fields[4].trim().toLongOrNull() ?: 0L,
        refs = parseDecorations(fields[5]),
        // A subject never contains the separators, but rejoin defensively if it somehow does.
        subject = fields.drop(FIELD_COUNT - 1).joinToString(FIELD_SEPARATOR.toString()),
    )
}

private fun parseDecorations(decorations: String): List<GitRef> =
    decorations.split(',')
        .map { entry -> entry.trim() }
        .filter { entry -> entry.isNotEmpty() }
        .map { entry -> parseDecoration(entry) }
        // origin/HEAD only mirrors the remote's default branch; showing it is noise.
        .filterNot { ref -> ref.kind == GitRef.Kind.RemoteBranch && ref.name.endsWith("/HEAD") }

private fun parseDecoration(entry: String): GitRef =
    when {
        entry == "HEAD" -> GitRef("HEAD", GitRef.Kind.Head, isCurrent = true)
        entry.startsWith("HEAD -> ") -> {
            val branch = entry.removePrefix("HEAD -> ")
            GitRef(branch.removePrefix(HEADS_PREFIX), GitRef.Kind.LocalBranch, isCurrent = true)
        }
        entry.startsWith("tag: ") -> GitRef(entry.removePrefix("tag: ").removePrefix(TAGS_PREFIX), GitRef.Kind.Tag)
        entry.startsWith(REMOTES_PREFIX) -> GitRef(entry.removePrefix(REMOTES_PREFIX), GitRef.Kind.RemoteBranch)
        entry.startsWith(HEADS_PREFIX) -> GitRef(entry.removePrefix(HEADS_PREFIX), GitRef.Kind.LocalBranch)
        entry.startsWith(TAGS_PREFIX) -> GitRef(entry.removePrefix(TAGS_PREFIX), GitRef.Kind.Tag)
        // Short-form decoration, in case a caller did not pass --decorate=full.
        else -> GitRef(entry, GitRef.Kind.LocalBranch)
    }

private const val HEADS_PREFIX = "refs/heads/"
private const val REMOTES_PREFIX = "refs/remotes/"
private const val TAGS_PREFIX = "refs/tags/"
