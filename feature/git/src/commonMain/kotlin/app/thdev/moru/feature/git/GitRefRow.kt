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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitBranchInfo
import app.thdev.moru.core.model.git.GitWorktreeInfo
import app.thdev.moru.feature.git.components.branches.GitRefContextMenuArea
import app.thdev.moru.feature.git.components.branches.GitRefMenuItem

/**
 * One branch in the sidebar, one compact line: the checked-out branch has a check, a branch out in another worktree a
 * folder mark. Clicking selects it; a right click opens [menu]. The full name stays in semantics when it is cut short.
 */
@Composable
internal fun GitRefRow(
    branch: GitBranchInfo,
    selected: Boolean,
    enabled: Boolean,
    onEvent: (GitManagementEvent) -> Unit,
    menu: () -> List<GitRefMenuItem> = { emptyList() },
    worktrees: List<GitWorktreeInfo> = emptyList(),
) {
    GitRefContextMenuArea(menu) {
        Row(
            Modifier.fillMaxWidth().height(24.dp)
                .testTag("git-select-branch-${branch.name}")
                .clip(RoundedCornerShape(MoruTheme.radius.small))
                .ckSurface(if (selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
                .clickable(enabled = enabled, role = Role.Button) { onEvent(GitManagementEvent.SelectBranch(branch.selectionKey)) }
                .semantics {
                    contentDescription = "${if (branch.isRemote) "Remote" else "Local"} branch ${branch.name}"
                    stateDescription = when {
                        branch.isCurrent -> "Current branch"
                        branch.checkedOutPath != null -> "Checked out in ${branch.checkedOutPath}"
                        selected -> "Selected branch"
                        else -> "Available branch"
                    }
                }
                .padding(start = 20.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The same marks as the branch picker; green where Git is, orange for another worktree's branch.
            val icon = gitBranchIcon(branch, worktrees)
            MoruIcon(icon, size = 13.dp, tint = when {
                branch.isCurrent -> MoruTheme.colors.success
                icon == MoruIconName.Worktree -> MoruTheme.colors.warning
                icon == MoruIconName.Cloud -> MoruTheme.colors.primary.copy(alpha = 0.85f)
                else -> MoruTheme.colors.textSecondary
            })
            MoruText(
                branch.name,
                Modifier.weight(1f),
                style = MoruTheme.typography.body.copy(fontWeight = if (branch.isCurrent) FontWeight.SemiBold else FontWeight.Normal),
                color = if (branch.isCurrent || selected) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
