package app.thdev.moru.feature.git

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButtonSize
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.core.model.git.GitWorkingFile

/**
 * One file under the unstaged or the staged heading: the kind of change as a symbol, the file name, then what changed
 * and its folder, cut short at the end rather than pushed out of the panel. A file staged in part is listed under
 * both headings; the staged copy takes its own tags so each stays addressable.
 */
@Composable
internal fun GitWorkingFileRow(
    file: GitWorkingFile,
    staged: Boolean,
    enabled: Boolean,
    revision: String,
    onEvent: (GitManagementEvent) -> Unit,
    onReview: ((String) -> Unit)? = null,
    open: Boolean = false,
) {
    val tagPath = if (staged && file.hasUnstaged) "staged-${file.path}" else file.path
    val name = file.path.substringAfterLast('/')
    val folder = file.path.substringBeforeLast('/', missingDelimiterValue = "")
    val status = if (staged) gitStatusLabel(file.indexStatus) else unstagedStatus(file)
    Row(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(MoruTheme.radius.small))
        .ckSurface(if (open) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row).padding(start = 6.dp, end = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        GitWorkingKindIcon(if (staged) stagedKind(file) else unstagedKind(file))
        val reviewModifier = if (onReview == null) Modifier else Modifier
            .testTag("git-review-$tagPath")
            .clickable(role = Role.Button, onClickLabel = "View diff for ${file.path}") { onReview(file.path) }
        // One line like a Git client's file list: the name, then where it is; the change kind is the icon, and only a
        // kind the icon cannot say (untracked, conflict, rename, type change) is also written out.
        Row(Modifier.weight(1f).then(reviewModifier).semantics { stateDescription = status },
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            MoruText(name, Modifier.weight(1f, fill = false).testTag("git-file-$tagPath"),
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val where = file.originalPath?.let { "from $it" } ?: folder
            if (where.isNotEmpty()) {
                MoruText(where, Modifier.weight(1f, fill = false), style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.StartEllipsis)
            }
            if (file.isConflict || file.isUntracked || file.originalPath != null || status == "Type changed") {
                MoruText(status, style = MoruTheme.typography.caption, maxLines = 1,
                    color = if (file.isConflict) MoruTheme.colors.error else MoruTheme.colors.textSecondary)
            }
        }
        if (staged) {
            MoruIconButton(MoruIconName.Minus, "Unstage ${file.path}",
                { onEvent(GitManagementEvent.ChangeIndex(GitAction.Unstage(gitLiteralPaths(listOf(file))), revision)) },
                Modifier.testTag("git-unstage-${file.path}"), enabled = enabled, size = MoruButtonSize.Small)
        } else {
            MoruIconButton(MoruIconName.Plus, "Stage ${file.path}",
                { onEvent(GitManagementEvent.ChangeIndex(GitAction.Stage(gitLiteralPaths(listOf(file))), revision)) },
                Modifier.testTag("git-stage-${file.path}"), MoruButtonType.SECONDARY, enabled, MoruButtonSize.Small)
        }
    }
}

private fun unstagedKind(file: GitWorkingFile): GitWorkingKind = when {
    file.isConflict -> GitWorkingKind.Modified
    file.isUntracked -> GitWorkingKind.Added
    file.worktreeStatus == 'D' -> GitWorkingKind.Deleted
    else -> GitWorkingKind.Modified
}

private fun stagedKind(file: GitWorkingFile): GitWorkingKind = when (file.indexStatus) {
    'A' -> GitWorkingKind.Added
    'D' -> GitWorkingKind.Deleted
    else -> GitWorkingKind.Modified
}

private fun unstagedStatus(file: GitWorkingFile): String = when {
    file.isConflict -> "Conflict (${file.indexStatus}${file.worktreeStatus})"
    file.isUntracked -> "Untracked"
    else -> gitStatusLabel(file.worktreeStatus)
}

private fun gitStatusLabel(status: Char): String = when (status) {
    'A' -> "Added"
    'M' -> "Modified"
    'D' -> "Deleted"
    'R' -> "Renamed"
    'C' -> "Copied"
    'T' -> "Type changed"
    else -> status.toString()
}
