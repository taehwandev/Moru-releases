package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.domain.git.ChangedCodePolicy

/** One row of a changed file read as code: a line by its number, or a folded run of [count] unchanged lines. */
internal sealed interface ChangedCodeEntry {
    data class Line(val number: Int) : ChangedCodeEntry
    data class Folded(val count: Int) : ChangedCodeEntry
}

/**
 * The rows of a [lineCount]-line file showing only what its change touched: each hunk's lines as the file is now,
 * with the context git kept around them, and every stretch between or around them folded. Hunks that touch or
 * overlap read as one. A change with no line in the file now (a file emptied out) leaves the whole file folded.
 */
internal fun changedCodeEntries(file: DiffFile, lineCount: Int): List<ChangedCodeEntry> {
    return ChangedCodePolicy.entries(file, lineCount).map { entry -> when (entry) {
        is ChangedCodePolicy.Entry.Line -> ChangedCodeEntry.Line(entry.number)
        is ChangedCodePolicy.Entry.Folded -> ChangedCodeEntry.Folded(entry.count)
    } }
}

internal fun selectedChangedFile(files: List<DiffFile>, preferred: String?): DiffFile? {
    val selected = ChangedCodePolicy.selectedPath(files.map { it.displayPath }, preferred)
    return files.firstOrNull { it.displayPath == selected }
}
