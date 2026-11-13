package app.thdev.moru.core.ui.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.ui.i18n.CoreUiStrings
import app.thdev.moru.core.ui.i18n.coreUiStrings

/** The changes of one file. A file with nothing to draw says why instead of showing a blank pane. */
@Composable
fun DiffFileView(file: DiffFile, modifier: Modifier = Modifier) {
    val emptyReason = emptyReasonOf(file, coreUiStrings())
    if (emptyReason != null) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MoruText(text = emptyReason, color = MoruTheme.colors.textSecondary)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        file.hunks.forEach { hunk ->
            item { DiffHunkHeaderRow(hunk.header) }
            items(hunk.lines) { line -> DiffLineRow(line) }
        }
    }
}

private fun emptyReasonOf(file: DiffFile, strings: CoreUiStrings): String? =
    when {
        file.isBinary -> strings.binaryFile
        file.hunks.isNotEmpty() -> null
        file.status == DiffFileStatus.Renamed -> strings.renamedOnly
        else -> strings.noLineChanges
    }
