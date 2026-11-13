package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffHunk
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind

private const val FILE_HEADER = "diff --git "
private val HUNK_HEADER = Regex("""^@@ -([0-9]+)(?:,[0-9]+)? \+([0-9]+)(?:,[0-9]+)? @@""")

/** Parses ordinary unified Git patches, preserving quoted literal paths and safely skipping malformed hunks. */
fun parseGitDiff(patch: String): List<DiffFile> {
    val files = mutableListOf<DiffFile>()
    var current: FileBuilder? = null
    var hunk: HunkBuilder? = null
    fun closeHunk() { hunk?.let { current?.hunks?.add(it.build()) }; hunk = null }
    fun closeFile() { closeHunk(); current?.build()?.let(files::add); current = null }
    for (line in patch.lineSequence()) {
        val file = current
        when {
            line.startsWith(FILE_HEADER) -> { closeFile(); current = FileBuilder.fromHeader(line.removePrefix(FILE_HEADER)) }
            file == null -> Unit
            hunk != null && line.startsWith("\\") -> Unit
            line.startsWith("@@") -> { closeHunk(); hunk = HunkBuilder.of(line) }
            hunk != null -> hunk?.add(line)
            else -> file.readExtendedHeader(line)
        }
    }
    closeFile()
    return files
}

private class HunkBuilder(private val header: String, private var oldNumber: Long, private var newNumber: Long) {
    private val lines = mutableListOf<DiffLine>()
    private fun number(value: Long): Int? = value.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
    fun add(raw: String) {
        val text = raw.drop(1)
        when {
            raw.startsWith("+") -> lines += DiffLine(DiffLineKind.Added, text, null, number(newNumber++))
            raw.startsWith("-") -> lines += DiffLine(DiffLineKind.Removed, text, number(oldNumber++), null)
            raw.startsWith(" ") -> lines += DiffLine(DiffLineKind.Context, text, number(oldNumber++), number(newNumber++))
        }
    }
    fun build() = DiffHunk(header, lines.toList())
    companion object {
        fun of(header: String): HunkBuilder? {
            val match = HUNK_HEADER.find(header) ?: return null
            val old = match.groupValues[1].toIntOrNull() ?: return null
            val new = match.groupValues[2].toIntOrNull() ?: return null
            return HunkBuilder(header, old.toLong(), new.toLong())
        }
    }
}

private class FileBuilder(var oldPath: String?, var newPath: String?) {
    val hunks = mutableListOf<DiffHunk>()
    private var status = DiffFileStatus.Modified
    private var binary = false
    private var invalidPath = false
    fun readExtendedHeader(line: String) {
        when {
            line.startsWith("new file mode") -> status = DiffFileStatus.Added
            line.startsWith("deleted file mode") -> status = DiffFileStatus.Deleted
            line.startsWith("rename from ") -> { status = DiffFileStatus.Renamed; oldPath = decoded(line.removePrefix("rename from ")) }
            line.startsWith("rename to ") -> newPath = decoded(line.removePrefix("rename to "))
            line.startsWith("--- ") -> oldPath = pathOf(line.removePrefix("--- "), "a/", oldPath)
            line.startsWith("+++ ") -> newPath = pathOf(line.removePrefix("+++ "), "b/", newPath)
            line.startsWith("Binary files ") || line.startsWith("GIT binary patch") -> binary = true
        }
    }
    private fun decoded(raw: String): String? = GitPathText.decode(raw).also { if (it == null) invalidPath = true }
    private fun pathOf(raw: String, prefix: String, fallback: String?): String? {
        val path = decoded(raw.substringBefore('\t')) ?: return null
        return if (path == "/dev/null") null else path.removePrefix(prefix).ifEmpty { fallback }
    }
    fun build(): DiffFile? = if (invalidPath) null else DiffFile(
        if (status == DiffFileStatus.Added) null else oldPath,
        if (status == DiffFileStatus.Deleted) null else newPath,
        status, hunks.toList(), binary,
    )
    companion object {
        fun fromHeader(rest: String): FileBuilder? {
            if (rest.startsWith('"')) {
                val first = GitPathText.token(rest, 0) ?: return null
                if (first.end >= rest.length || rest[first.end] != ' ') return null
                val remaining = rest.substring(first.end).trimStart(' ')
                val second = GitPathText.decode(remaining) ?: return null
                return FileBuilder(first.value.removePrefix("a/"), second.removePrefix("b/"))
            }
            val quotedSecond = rest.indexOf(" \"b/")
            if (quotedSecond >= 0) {
                val second = GitPathText.decode(rest.substring(quotedSecond + 1)) ?: return null
                return FileBuilder(rest.substring(0, quotedSecond).removePrefix("a/"), second.removePrefix("b/"))
            }
            val separators = Regex(" b/").findAll(rest).map { it.range.first }.toList()
            val split = separators.firstOrNull { index ->
                rest.substring(0, index).removePrefix("a/") == rest.substring(index + 1).removePrefix("b/")
            } ?: separators.firstOrNull()
            if (split == null) return GitPathText.decode(rest)?.let { FileBuilder(it, it) }
            return FileBuilder(rest.substring(0, split).removePrefix("a/"), rest.substring(split + 1).removePrefix("b/"))
        }
    }
}
