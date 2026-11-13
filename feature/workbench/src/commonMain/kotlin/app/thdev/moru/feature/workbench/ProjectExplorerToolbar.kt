package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.CodeExplorerSort
import app.thdev.moru.feature.workbench.i18n.codeStrings

/**
 * The explorer's header: its title and folder, then its tools: show the open file, open every folder, close them all,
 * the options menu (compact rows, always show the open file, order, ignored files, merged folder paths) and a new note.
 */
@Composable
internal fun ProjectExplorerToolbar(
    workspacePath: String,
    canLocate: Boolean,
    onLocate: () -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    options: CodeExplorerOptions,
    onOptionsChange: (CodeExplorerOptions) -> Unit,
    composingNote: Boolean,
    onToggleNote: () -> Unit,
) {
    val strings = codeStrings()
    var optionsOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MoruText(
                text = strings.explorerTitle,
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            MoruText(
                text = workspacePath.substringAfterLast('/').ifBlank { workspacePath },
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
            )
        }
        MoruIconButton(
            onClick = onLocate,
            description = strings.locateOpenFile,
            size = 26.dp,
        ) {
            val tint = if (canLocate) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary.copy(alpha = 0.5f)
            MoruIcon(MoruIconName.Target, size = 14.dp, tint = tint)
        }
        MoruIconButton(onClick = onExpandAll, description = strings.expandAll, size = 26.dp) {
            MoruIcon(MoruIconName.ExpandAll, size = 14.dp)
        }
        MoruIconButton(
            onClick = onCollapseAll,
            description = strings.collapseAll,
            size = 26.dp,
        ) { MoruIcon(MoruIconName.CollapseAll, size = 14.dp) }
        Box {
            MoruIconButton(onClick = { optionsOpen = true }, description = strings.explorerOptions, size = 26.dp) {
                MoruIcon(MoruIconName.Ellipsis, size = 14.dp)
            }
            MoruDropdownMenu(
                expanded = optionsOpen,
                onDismiss = { optionsOpen = false },
                items = listOf(
                    MoruMenuItem(strings.compactRows, checked = options.compact) {
                        onOptionsChange(options.copy(compact = !options.compact))
                    },
                    MoruMenuItem(strings.followOpenFile, checked = options.followOpenFile) {
                        onOptionsChange(options.copy(followOpenFile = !options.followOpenFile))
                    },
                    MoruMenuItem(strings.sortByName, checked = options.sortBy == CodeExplorerSort.Name) {
                        onOptionsChange(options.copy(sortBy = CodeExplorerSort.Name))
                    },
                    MoruMenuItem(strings.sortByType, checked = options.sortBy == CodeExplorerSort.Type) {
                        onOptionsChange(options.copy(sortBy = CodeExplorerSort.Type))
                    },
                    MoruMenuItem(strings.foldersFirst, checked = options.foldersFirst) {
                        onOptionsChange(options.copy(foldersFirst = !options.foldersFirst))
                    },
                    MoruMenuItem(strings.showIgnored, checked = options.showIgnored) {
                        onOptionsChange(options.copy(showIgnored = !options.showIgnored))
                    },
                    MoruMenuItem(strings.mergeSingleFolders, checked = options.mergeSingleFolders) {
                        onOptionsChange(options.copy(mergeSingleFolders = !options.mergeSingleFolders))
                    },
                ),
            )
        }
        // An icon like the tools beside it, so the explorer's title keeps its room.
        MoruIconButton(
            onClick = onToggleNote,
            description = if (composingNote) strings.closeNewNote else strings.newNote,
            size = 26.dp,
        ) { MoruIcon(if (composingNote) MoruIconName.Close else MoruIconName.Plus, size = 14.dp) }
    }
}
