package app.thdev.moru.feature.git.history.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.ui.diff.DiffFileRow
import app.thdev.moru.core.ui.diff.DiffFileView
import app.thdev.moru.core.ui.i18n.CoreUiStrings
import app.thdev.moru.core.ui.i18n.coreUiStrings

private const val FileListShare = 0.3f

/**
 * The changed files of a diff above and the chosen file's diff below, both full width so paths and long lines are not
 * cut short. The list takes what its files need up to [FileListShare] of the height and scrolls past that. Shared by
 * the commit view and the uncommitted-changes view. Only the open file is local state, reset for a new set of files;
 * [diff] must have at least one file.
 */
@Composable
internal fun DiffFilesBrowser(diff: CommitDiffResult.Loaded, modifier: Modifier = Modifier) {
    var selectedPath by remember(diff.files.map { file -> file.displayPath }) { mutableStateOf<String?>(null) }
    val selectedIndex = diff.files.indexOfFirst { file -> file.displayPath == selectedPath }.coerceAtLeast(0)
    Column(modifier = modifier.fillMaxSize()) {
        MoruText(
            text = diffTotals(diff, coreUiStrings()),
            style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val listMaxHeight = maxHeight * FileListShare
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = listMaxHeight)) {
                    itemsIndexed(diff.files) { index, file ->
                        DiffFileRow(
                            file = file,
                            selected = index == selectedIndex,
                            onClick = { selectedPath = file.displayPath },
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().ckSurface(MoruSurfaceRole.Panel)) {
                    DiffFileView(diff.files[selectedIndex])
                }
            }
        }
    }
}

private fun diffTotals(diff: CommitDiffResult.Loaded, strings: CoreUiStrings): String {
    val additions = diff.files.sumOf { file -> file.additions }
    val deletions = diff.files.sumOf { file -> file.deletions }
    return strings.filesSummary(diff.files.size, additions, deletions, diff.truncated)
}
