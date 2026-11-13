package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.ui.diff.DiffFileRow

private const val FileListShare = 0.35f

/** Shows one changed file's code; the caller decides where its content comes from. */
internal typealias ChangedFileCodeSlot = @Composable (worktreeRoot: String, file: DiffFile, modifier: Modifier) -> Unit

/**
 * The changed files above and the chosen one's code below. The list takes what its files need up to [FileListShare] of
 * the height. The chosen file is kept while the changes are reread, and the first file is shown when it is gone.
 */
@Composable
internal fun ChangedFilesBrowser(
    worktreeRoot: String,
    changes: CommitDiffResult.Loaded,
    fileCode: ChangedFileCodeSlot,
    modifier: Modifier = Modifier,
) {
    var selectedPath by remember(worktreeRoot) { mutableStateOf<String?>(null) }
    val selected = checkNotNull(selectedChangedFile(changes.files, selectedPath))
    BoxWithConstraints(modifier.fillMaxSize()) {
        val listMaxHeight = maxHeight * FileListShare
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = listMaxHeight)) {
                items(changes.files, key = { file -> file.displayPath }) { file ->
                    DiffFileRow(file = file, selected = file == selected, onClick = { selectedPath = file.displayPath })
                }
            }
            fileCode(worktreeRoot, selected, Modifier.weight(1f).fillMaxWidth().ckSurface(MoruSurfaceRole.Page))
        }
    }
}
