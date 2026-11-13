package app.thdev.moru.feature.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.thdev.moru.core.model.git.GitRepositorySnapshot
import app.thdev.moru.core.ui.path.LocalHomeDirectory
import app.thdev.moru.core.ui.path.homeRelativePath

/**
 * The top of the changes panel: which branch a commit lands on and in which checkout folder, and how many files are
 * changed there. With several worktrees of one repository, this is what says the changes below are this folder's.
 */
@Composable
internal fun GitCommitTarget(snapshot: GitRepositorySnapshot, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("git-commit-target"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            MoruText("${formatGitCount(snapshot.files.size)} changed on", style = MoruTheme.typography.body,
                color = MoruTheme.colors.textSecondary, maxLines = 1)
            MoruIcon(MoruIconName.Branch, size = 13.dp, tint = MoruTheme.colors.primary)
            MoruText(gitBranchLabel(snapshot), Modifier.weight(1f, fill = false),
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MoruText(homeRelativePath(snapshot.rootPath, LocalHomeDirectory.current), style = MoruTheme.typography.caption,
            color = MoruTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.StartEllipsis)
    }
}
