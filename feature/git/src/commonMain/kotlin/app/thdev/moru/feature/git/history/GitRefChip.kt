package app.thdev.moru.feature.git.history

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

import app.thdev.moru.core.model.git.history.GitRef
/**
 * A branch, tag or HEAD label in the lane [color] of its commit. The checked-out ref is
 * solid, local refs are tinted, and remote refs are fainter so they read as secondary.
 * Meaning never rests on color alone: tags carry a marker, HEAD says HEAD, and another worktree shows a folder with
 * the count of files it has changed and not committed yet.
 */
@Composable
internal fun GitRefChip(
    ref: GitRef,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val solid = ref.isCurrent
    val background = when {
        solid -> color
        ref.kind == GitRef.Kind.RemoteBranch -> color.copy(alpha = 0.10f)
        else -> color.copy(alpha = 0.18f)
    }
    val content = when {
        !solid -> color
        MoruTheme.colors.isLight -> Color.White
        else -> MoruTheme.colors.background
    }
    val label = when (ref.kind) {
        GitRef.Kind.Tag -> ref.name
        GitRef.Kind.Head -> "HEAD"
        else -> ref.name
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MoruTheme.radius.xSmall))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        when (ref.kind) {
            GitRef.Kind.Tag -> MoruIcon(name = MoruIconName.Tag, size = 11.dp, tint = content)
            GitRef.Kind.LocalBranch, GitRef.Kind.Head -> MoruIcon(name = MoruIconName.Branch, size = 11.dp, tint = content)
            GitRef.Kind.Worktree -> MoruIcon(name = MoruIconName.Folder, size = 11.dp, tint = content)
            GitRef.Kind.RemoteBranch -> Unit
        }
        MoruText(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = MoruTheme.typography.caption.copy(
                fontWeight = if (solid) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Work still going on in that worktree: files changed there and not committed yet.
        if (ref.pendingChanges > 0) {
            MoruIcon(name = MoruIconName.Pencil, size = 10.dp, tint = MoruTheme.colors.warning)
            MoruText(ref.pendingChanges.toString(), style = MoruTheme.typography.caption, color = MoruTheme.colors.warning,
                maxLines = 1)
        }
      }
    }
}
