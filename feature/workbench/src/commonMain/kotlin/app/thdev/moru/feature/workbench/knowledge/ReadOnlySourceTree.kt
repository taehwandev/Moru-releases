package app.thdev.moru.feature.workbench.knowledge

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.workbench.ProjectTreeRow
import app.thdev.moru.feature.workbench.QuickOpenRow
import app.thdev.moru.feature.workbench.projectTreeNameStyle
import app.thdev.moru.feature.workbench.quickOpenMatches
import app.thdev.moru.feature.workbench.rememberProjectTreeContentWidth
import app.thdev.moru.feature.workbench.visibleProjectTreeRows

/** Uses Code's exact tree rows, selection styling, width measurement and quick-open rows. */
@Composable
fun ReadOnlySourceTree(
    sourceKey: String,
    paths: List<String>,
    selectedPath: String?,
    query: String,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(paths) { sourceDocumentEntries(paths) }
    var expanded by remember(sourceKey) { mutableStateOf(emptySet<String>()) }
    var reveal by remember(sourceKey) { mutableStateOf<String?>(null) }
    val rows = remember(entries, expanded) { visibleProjectTreeRows(entries, expanded, emptyMap()) }
    val matches = remember(paths, query) { quickOpenMatches(query, paths) }
    val listState = rememberLazyListState()
    LaunchedEffect(sourceKey, selectedPath) {
        selectedPath?.let { path ->
            expanded += generateSequence(path.substringBeforeLast('/', "")) { it.substringBeforeLast('/', "") }.takeWhile { it.isNotEmpty() }.toSet()
            reveal = path
        }
    }
    LaunchedEffect(rows, reveal) {
        val index = rows.indexOfFirst { it.path == reveal }
        if (index >= 0) { listState.scrollToItem((index - 3).coerceAtLeast(0)); reveal = null }
    }
    val width = rememberProjectTreeContentWidth(rows, projectTreeNameStyle(selected = true))
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (query.isBlank()) LazyColumn(
            state = listState,
            modifier = Modifier.horizontalScroll(rememberScrollState()).width(maxOf(maxWidth, width)).fillMaxHeight().padding(vertical = MoruTheme.spacing.xSmall),
        ) {
            items(rows, key = { it.path }) { entry ->
                ProjectTreeRow(entry, entry.path in expanded, entry.path == selectedPath, change = null,
                    onClick = {
                        if (entry.isFile) onOpenFile(entry.path)
                        else expanded = if (entry.path in expanded) expanded - entry.path else expanded + entry.path
                    })
            }
        } else LazyColumn(Modifier.fillMaxWidth().fillMaxHeight()) {
            if (matches.isEmpty()) item { MoruText("No matching files.", Modifier.padding(MoruTheme.spacing.medium), color = MoruTheme.colors.textSecondary) }
            items(matches, key = { it }) { path -> QuickOpenRow(path, path == selectedPath, onOpenFile) }
        }
    }
}
