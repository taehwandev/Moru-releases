package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.GitAction
import app.thdev.moru.feature.git.components.navigation.GitTargetSelector

/**
 * "repository › branch": the repository Git acts on and the branch commits go to, picked from a searchable list whose
 * icons tell the kinds apart (see [gitBranchIcon]). Picking a plain local branch asks to switch to it, so later commits
 * land there; switching needs a clean folder, so those are offered only then. A branch checked out in another worktree
 * cannot be checked out twice, so picking it opens that worktree in Git instead.
 */
@Composable
internal fun GitRepositoryBranchBar(state: GitManagementUiState, onEvent: (GitManagementEvent) -> Unit, modifier: Modifier = Modifier) {
    val snapshot = state.snapshot
    val current = snapshot?.let(::gitBranchLabel).orEmpty()
    val clean = snapshot != null && snapshot.files.isEmpty() && snapshot.operationInProgress == null
    val local = snapshot?.branches.orEmpty().filter { branch -> !branch.isRemote }
    val worktrees = snapshot?.worktrees.orEmpty()
    val icons = local.associate { branch -> branch.name to gitBranchIcon(branch, worktrees) }
    // A branch out in another checkout (the main one or a linked worktree) is opened there rather than switched to.
    val elsewhere = local.filter { !it.isCurrent }.mapNotNull { branch ->
        val path = worktrees.firstOrNull { it.branch == branch.name }?.path ?: branch.checkedOutPath
        path?.takeIf { it != snapshot?.rootPath }?.let { branch.name to it }
    }.toMap()
    // Here first, then the other checkouts, then the branches one switch away (offered only on a clean folder).
    val options = local
        .filter { branch -> branch.isCurrent || branch.name in elsewhere || clean }
        .sortedBy { branch -> when { branch.isCurrent -> 0; branch.name in elsewhere -> 1; else -> 2 } }
        .map { branch -> branch.name }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            MoruText("repository", style = MoruTheme.typography.micro, color = MoruTheme.colors.textSecondary)
            MoruText(
                // The repository's name is its main checkout's folder, even while Git is on a linked worktree.
                (worktrees.firstOrNull { it.isMain }?.path ?: snapshot?.rootPath)?.substringAfterLast('/').orEmpty(),
                Modifier.testTag("git-repository-name"),
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoruIcon(MoruIconName.ChevronRight, size = 14.dp, tint = MoruTheme.colors.textSecondary)
        Column {
            MoruText("branch", style = MoruTheme.typography.micro, color = MoruTheme.colors.textSecondary)
            GitTargetSelector("Branch", current, options, state.interactive, "git-branch-picker",
                { picked ->
                    val worktree = elsewhere[picked]
                    when {
                        worktree != null -> onEvent(GitManagementEvent.OpenWorktree(worktree))
                        picked != snapshot?.branchName -> onEvent(GitManagementEvent.Propose(GitAction.SwitchBranch(picked)))
                    }
                },
                Modifier.width(220.dp),
                iconOf = { name -> icons[name] },
                searchPlaceholder = "Search")
        }
    }
}
