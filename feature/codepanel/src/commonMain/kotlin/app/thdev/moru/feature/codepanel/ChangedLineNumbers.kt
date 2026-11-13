package app.thdev.moru.feature.codepanel

import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.domain.git.ChangedCodePolicy
/** The line numbers of [file] as it is now that its change added or rewrote. */
internal fun changedLineNumbers(file: DiffFile): Set<Int> =
    ChangedCodePolicy.changedLineNumbers(file)
