package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString

/** A file's [entries] (its lines with their numbers, the [changed] ones marked, and folded unchanged runs). */
@Composable
internal fun CodeLines(
    lines: List<AnnotatedString>,
    entries: List<ChangedCodeEntry>,
    changed: Set<Int>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(entries) { entry ->
            when (entry) {
                is ChangedCodeEntry.Line -> lines.getOrNull(entry.number - 1)?.let { text ->
                    CodeLineRow(number = entry.number, text = text, changed = entry.number in changed)
                }
                is ChangedCodeEntry.Folded -> FoldedCodeRow(entry.count)
            }
        }
    }
}
