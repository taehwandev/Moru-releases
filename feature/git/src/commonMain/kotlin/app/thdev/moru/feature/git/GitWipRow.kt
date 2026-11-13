package app.thdev.moru.feature.git

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.moru.core.ui.path.LocalHomeDirectory
import app.thdev.moru.core.ui.path.homeRelativePath
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitWorkingFile
import app.thdev.moru.feature.git.i18n.historyStrings

/**
 * The row above the commits that stands for what is not committed yet; selecting it shows the changes to stage.
 * An accent edge and a rule under it set it off from the commits, and it counts changed, added and deleted files
 * apart, each with its own symbol. It names the [branch] and [folder] those changes are in, so with several worktrees
 * it is clear which checkout this view reads.
 */
@Composable
internal fun GitWipRow(
    files: List<GitWorkingFile>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    branch: String? = null,
    folder: String? = null,
) {
    val strings = historyStrings()
    val colors = MoruTheme.colors
    val accent = if (selected) colors.primary else colors.primary.copy(alpha = 0.45f)
    val tally = gitWorkingTally(files)
    Row(
        modifier = modifier.fillMaxWidth().height(36.dp).testTag("git-wip-row")
            .ckSurface(if (selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
            .drawBehind {
                drawRect(accent, size = size.copy(width = 3.dp.toPx()))
                val rule = 1.dp.toPx()
                drawRect(colors.border, topLeft = Offset(0f, size.height - rule), size = size.copy(height = rule))
            }
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = strings.workingChanges(files.size)
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(start = 14.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruIcon(MoruIconName.Circle, size = 12.dp, tint = accent)
        MoruText(
            if (files.isEmpty()) strings.workingChanges(0) else strings.workingFolder,
            style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = if (files.isEmpty()) colors.textSecondary else colors.textPrimary,
            maxLines = 1,
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (branch != null) {
                MoruIcon(MoruIconName.Branch, size = 12.dp, tint = colors.textSecondary)
                MoruText(branch, Modifier.weight(1f, fill = false).testTag("git-wip-branch"), style = MoruTheme.typography.body,
                    color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (folder != null) {
                MoruText(homeRelativePath(folder, LocalHomeDirectory.current), Modifier.weight(1f, fill = false).testTag("git-wip-folder"),
                    style = MoruTheme.typography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TallyCount(GitWorkingKind.Modified, tally.modified)
        TallyCount(GitWorkingKind.Added, tally.added)
        TallyCount(GitWorkingKind.Deleted, tally.deleted)
    }
}

@Composable
private fun TallyCount(kind: GitWorkingKind, count: Int) {
    if (count == 0) return
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        GitWorkingKindIcon(kind)
        MoruText(formatGitCount(count), style = MoruTheme.typography.body, maxLines = 1)
    }
}
