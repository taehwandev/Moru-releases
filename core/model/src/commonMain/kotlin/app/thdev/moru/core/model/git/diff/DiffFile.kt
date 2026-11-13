package app.thdev.moru.core.model.git.diff

/**
 * One file in a diff. [oldPath] is null for an added file and [newPath] is null for a deleted
 * one. A binary file has no hunks: [isBinary] says why, so the viewer does not look empty.
 */
data class DiffFile(
    val oldPath: String?,
    val newPath: String?,
    val status: DiffFileStatus,
    val hunks: List<DiffHunk>,
    val isBinary: Boolean,
) {
    /** The path to show: where the file lives after the change, or where it lived before a delete. */
    val displayPath: String get() = newPath ?: oldPath.orEmpty()

    val additions: Int get() = hunks.sumOf { hunk -> hunk.lines.count { line -> line.kind == DiffLineKind.Added } }

    val deletions: Int get() = hunks.sumOf { hunk -> hunk.lines.count { line -> line.kind == DiffLineKind.Removed } }
}
