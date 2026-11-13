package app.thdev.moru.feature.codepanel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruPaneHeader
import app.thdev.moru.core.designsystem.component.MoruEmptyState
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.feature.codepanel.i18n.codePanelStrings
import app.thdev.moru.core.ui.i18n.coreUiStrings

/**
 * The code a terminal is changing: which checkout it reads, the files changed there and the chosen file's code.
 * [changes] is null while a reading runs; [worktreeRoot] is null until a checkout is known, and stays null with no
 * [worktrees] once [checked] says the folder is not in git.
 */
@Composable
internal fun CodePanel(
    source: CodePanelSource,
    worktrees: List<CodeWorktree>,
    checked: Boolean,
    worktreeRoot: String?,
    changes: CommitDiffResult?,
    onPickWorktree: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = codePanelStrings()
    Column(modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Panel)) {
        Row(
            Modifier.fillMaxWidth().height(MoruPaneHeader.height).padding(horizontal = MoruPaneHeader.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorktreePicker(worktrees, worktreeRoot, onPickWorktree)
        }
        val loaded = changes as? CommitDiffResult.Loaded
        if (loaded != null && loaded.files.isNotEmpty() && worktreeRoot != null) {
            MoruText(
                coreUiStrings().filesSummary(
                    loaded.files.size,
                    loaded.files.sumOf { file -> file.additions },
                    loaded.files.sumOf { file -> file.deletions },
                    loaded.truncated,
                ),
                style = MoruTheme.typography.caption,
                color = MoruTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            ChangedFilesBrowser(source, worktreeRoot, loaded, Modifier.weight(1f))
        } else {
            val message = when {
                checked && worktrees.isEmpty() -> strings.notRepository
                changes is CommitDiffResult.Unavailable -> changes.message
                loaded != null -> strings.noChanges
                else -> strings.loading
            }
            MoruEmptyState(message, Modifier.weight(1f).fillMaxWidth(), icon = MoruIconName.Code)
        }
    }
}
