package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruSyntaxColors
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import app.thdev.moru.core.ui.code.CodeLanguage
import app.thdev.moru.core.ui.code.codeLines
import app.thdev.moru.core.ui.code.highlightCode
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.ui.diff.DiffFileView
import app.thdev.moru.feature.codepanel.i18n.codePanelStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One changed file, read as code: only the ranges its change touched, as the file is now with the written lines
 * marked and the unchanged stretches folded (the whole file is in the Code tab), or as a diff. A deleted file has no
 * code left, so it shows its diff. The file is read again whenever its change moves.
 */
@Composable
internal fun ChangedFileCode(
    source: CodePanelSource,
    worktreeRoot: String,
    file: DiffFile,
    modifier: Modifier = Modifier,
) {
    val strings = codePanelStrings()
    val deleted = file.status == DiffFileStatus.Deleted || file.newPath == null
    var showDiff by remember(file.displayPath) { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(MoruPaneHeader.height).padding(horizontal = MoruPaneHeader.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoruText(
                file.displayPath.substringAfterLast('/'),
                style = MoruTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!deleted) {
                CodeModeButton(strings.codeMode, selected = !showDiff) { showDiff = false }
                CodeModeButton(strings.diffMode, selected = showDiff) { showDiff = true }
            }
        }
        if (deleted || showDiff) {
            DiffFileView(file, Modifier.weight(1f))
        } else {
            val preview by produceState<WorkbenchFilePreview?>(null, worktreeRoot, file) {
                value = withContext(Dispatchers.IO) { source.readFile(worktreeRoot, file.displayPath) }
            }
            val content = preview?.content
            if (content == null) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                    MoruText(preview?.summaryText ?: strings.loading, color = MoruTheme.colors.textSecondary)
                }
            } else {
                val syntax = MoruSyntaxColors.of(MoruTheme.colors)
                val lines = remember(content, file.displayPath, syntax) {
                    highlightCode(content, CodeLanguage.of(file.displayPath), syntax).codeLines()
                }
                val entries = remember(file, lines.size) { changedCodeEntries(file, lines.size) }
                CodeLines(lines, entries, changedLineNumbers(file), Modifier.weight(1f))
            }
        }
    }
}
