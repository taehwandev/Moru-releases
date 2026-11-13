package app.thdev.moru.feature.git.components.branches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitWorktreeInfo
import app.thdev.moru.core.ui.path.LocalHomeDirectory
import app.thdev.moru.core.ui.path.homeRelativePath

/**
 * One checkout of the repository: its branch (or detached commit) and folder. The one this Git view reads is marked
 * "This view", so the working folder row is never mistaken for another checkout; a folder Git lost is marked
 * "Missing". Clicking selects its branch; a right click opens [menu]. It never opens or changes the folder itself.
 */
@Composable
internal fun GitWorktreeRow(
    worktree: GitWorktreeInfo,
    viewed: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean,
    menu: () -> List<GitRefMenuItem> = { emptyList() },
) {
    val name = worktree.branch ?: "Detached ${worktree.head?.take(7).orEmpty()}".trim()
    val displayPath = homeRelativePath(worktree.path, LocalHomeDirectory.current)
    GitRefContextMenuArea(menu) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("git-worktree-${worktree.branch ?: worktree.path.substringAfterLast('/')}")
                .clip(RoundedCornerShape(MoruTheme.radius.small))
                .ckSurface(if (viewed) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
                .clickable(enabled = enabled, role = Role.Button, onClick = onSelect)
                .semantics {
                    contentDescription = "Worktree $name at ${worktree.path}${if (viewed) ", shown in this view" else ""}"
                }
                .padding(start = 20.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The same marks as the branch picker: a house for the main checkout, trees for a linked worktree.
            MoruIcon(if (worktree.isMain) MoruIconName.Home else MoruIconName.Worktree, size = 13.dp, tint = when {
                worktree.prunable -> MoruTheme.colors.error
                viewed -> MoruTheme.colors.success
                else -> MoruTheme.colors.textSecondary
            })
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MoruText(name, Modifier.weight(1f, fill = false),
                        style = MoruTheme.typography.body.copy(fontWeight = if (viewed) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (viewed) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val mark = when {
                        worktree.prunable -> "MISSING" to MoruTheme.colors.error
                        viewed -> "THIS VIEW" to MoruTheme.colors.success
                        worktree.isMain -> "MAIN" to MoruTheme.colors.textSecondary
                        else -> null
                    }
                    mark?.let { (text, color) ->
                        MoruText(text, style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.Bold), color = color, maxLines = 1)
                    }
                }
                // Worktree folders share a long parent, so the start is cut and the folder's own name stays.
                MoruText(displayPath, style = MoruTheme.typography.caption, color = MoruTheme.colors.textSecondary,
                    maxLines = 1, overflow = TextOverflow.StartEllipsis)
            }
        }
    }
}
