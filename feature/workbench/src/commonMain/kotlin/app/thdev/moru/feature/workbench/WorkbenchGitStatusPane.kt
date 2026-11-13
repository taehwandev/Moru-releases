package app.thdev.moru.feature.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruBadge
import app.thdev.moru.core.designsystem.component.MoruBadgeType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.WorkbenchGitChange
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.feature.workbench.i18n.codeStrings
import app.thdev.moru.feature.workbench.i18n.CodeStrings

@Composable
internal fun WorkbenchGitStatusPane(
    status: WorkbenchGitStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = codeStrings()
    Column(
        modifier = modifier
            .ckSurface(MoruSurfaceRole.Panel)
            .padding(top = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruText(
                text = strings.gitTitle,
                modifier = Modifier.weight(1f),
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            MoruText(
                text = strings.refresh,
                modifier = Modifier
                    .ckSurface(MoruSurfaceRole.Row)
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption,
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruBadge(
                text = status.branchName,
                type = if (status.repositoryDetected) MoruBadgeType.INFO else MoruBadgeType.NEUTRAL,
            )
            if (status.changedCount > 0) {
                MoruBadge(text = strings.changedCount(status.changedCount), type = MoruBadgeType.WARNING)
            } else {
                MoruBadge(text = strings.clean, type = MoruBadgeType.SUCCESS)
            }
        }

        val summary = status.summaryLine(strings)
        MoruText(
            text = summary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
        )

        val visibleChanges = status.changes.take(MaxVisibleGitChanges)
        if (visibleChanges.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(
                    items = visibleChanges,
                    key = { change -> "${change.shortStatus}:${change.path}" },
                ) { change ->
                    GitChangeRow(change = change)
                }
            }
        }
    }
}

@Composable
private fun GitChangeRow(change: WorkbenchGitChange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ckSurface(MoruSurfaceRole.Row)
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(
            text = change.shortStatus,
            color = change.badgeColor(),
            style = MoruTheme.typography.caption.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
        MoruText(
            text = change.path,
            modifier = Modifier.weight(1f),
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
        )
    }
}

@Composable
private fun WorkbenchGitChange.badgeColor() = when {
    isUntracked -> MoruTheme.colors.primary
    indexStatus == 'D' || worktreeStatus == 'D' -> MoruTheme.colors.error
    indexStatus != null -> MoruTheme.colors.success
    else -> MoruTheme.colors.warning
}

private fun WorkbenchGitStatus.summaryLine(strings: CodeStrings): String {
    val statusMessage = message
    if (!statusMessage.isNullOrBlank()) return statusMessage
    return strings.gitSummary(stagedCount, unstagedCount, untrackedCount)
}

private const val MaxVisibleGitChanges = 8
