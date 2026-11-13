package app.thdev.moru.core.ui.code

import androidx.compose.ui.text.AnnotatedString
import app.thdev.moru.core.domain.git.ChangedCodePolicy

/** [this] split into its lines, each keeping its colors; a trailing newline does not add an empty last line. */
fun AnnotatedString.codeLines(): List<AnnotatedString> =
    ChangedCodePolicy.lineSlices(text).map { subSequence(it.start, it.end) }
