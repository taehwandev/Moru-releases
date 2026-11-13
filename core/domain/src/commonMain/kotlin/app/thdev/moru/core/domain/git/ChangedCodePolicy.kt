package app.thdev.moru.core.domain.git

import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffLineKind

/** Portable interpretation of the inline Code view's current-file rows, without UI or file reads. */
object ChangedCodePolicy {
    sealed interface Entry {
        data class Line(val number: Int) : Entry
        data class Folded(val count: Int) : Entry
    }
    data class LineSlice(val start: Int, val end: Int)

    /** Empty content is one blank line; a final newline does not introduce a phantom last row. */
    fun lineSlices(content: String): List<LineSlice> {
        val lines = mutableListOf<LineSlice>()
        var start = 0
        while (start <= content.length) {
            val end = content.indexOf('\n', start).let { if (it < 0) content.length else it }
            if (end == content.length && start == content.length && lines.isNotEmpty()) break
            lines += LineSlice(start, end)
            start = end + 1
        }
        return lines
    }

    fun changedLineNumbers(file: DiffFile): Set<Int> = file.hunks.flatMap { it.lines }
        .filter { it.kind == DiffLineKind.Added }.mapNotNull { it.newNumber }.toSet()

    /** Uses the hunk's existing context, merges touching ranges and clips to the available preview lines. */
    fun entries(file: DiffFile, lineCount: Int): List<Entry> {
        if (lineCount <= 0) return emptyList()
        val ranges = file.hunks.mapNotNull { hunk ->
            val numbers = hunk.lines.mapNotNull { it.newNumber }.filter { it in 1..lineCount }
            if (numbers.isEmpty()) null else numbers.min()..numbers.max()
        }.sortedBy { it.first }.fold(mutableListOf<IntRange>()) { merged, range ->
            val last = merged.lastOrNull()
            if (last != null && range.first.toLong() <= last.last.toLong() + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            } else merged += range
            merged
        }
        val result = mutableListOf<Entry>()
        var next = 1L
        for (range in ranges) {
            if (range.first > next) result += Entry.Folded((range.first - next).toInt())
            range.forEach { result += Entry.Line(it) }
            next = range.last.toLong() + 1
        }
        if (next <= lineCount) result += Entry.Folded((lineCount - next + 1).toInt())
        return result
    }

    fun selectedPath(paths: List<String>, preferred: String?): String? = paths.firstOrNull { it == preferred } ?: paths.firstOrNull()
}
