package app.thdev.moru.feature.git.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.history.GitRef
import app.thdev.moru.core.ui.time.formatRelativeTime
import app.thdev.moru.core.ui.i18n.coreUiStrings

private val RowHeight = 30.dp
private val ChipMaxWidth = 150.dp
private const val VisibleRefs = 2

/**
 * One commit: the graph cell, the branch and tag labels in front of the subject, then author, age and short hash in
 * fixed columns. [layout] says how wide the graph is and which columns a narrow pane drops. The checked-out commit's
 * subject is bold. Stateless; selection is owned by the caller.
 */
@Composable
internal fun GitCommitRowView(
    row: GitGraphRow,
    laneColors: List<Color>,
    graphLaneCount: Int,
    layout: GitRowLayout,
    selected: Boolean,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commit = row.commit
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val surface = MoruTheme.surfaceStyle(if (selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
    val background = when {
        selected -> surface.background
        hovered -> MoruTheme.colors.textPrimary.copy(alpha = 0.05f)
        else -> surface.background
    }
    val rowBackground = background.compositeOver(MoruTheme.colors.background)
    val laneColor = laneColors[row.nodeColor.mod(laneColors.size)]
    val age = formatRelativeTime(coreUiStrings(), nowEpochSeconds, commit.epochSeconds)
    val current = commit.refs.any { ref -> ref.isCurrent }
    val secondary = MoruTheme.colors.textSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(background)
            .hoverable(interaction)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${commit.subject}, ${commit.authorName}, $age, ${commit.shortHash}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GitGraphCanvas(
            row = row,
            laneColors = laneColors,
            laneCount = graphLaneCount,
            width = layout.graphWidth,
            haloColor = rowBackground,
            isCurrent = current,
            modifier = Modifier.padding(start = 6.dp),
        )
        Row(
            modifier = Modifier.weight(1f).padding(start = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checked-out first, then other worktrees, local branches, tags (version marks), then remotes.
            val refs = commit.refs.sortedWith(compareByDescending<GitRef> { it.isCurrent }.thenBy { it.kind.displayPriority() })
            refs.take(VisibleRefs).forEach { ref ->
                GitRefChip(ref = ref, color = laneColor, modifier = Modifier.widthIn(max = ChipMaxWidth))
            }
            if (refs.size > VisibleRefs) {
                MoruText("+${refs.size - VisibleRefs}", style = MoruTheme.typography.caption, color = secondary, maxLines = 1)
            }
            MoruText(
                text = commit.subject,
                modifier = Modifier.weight(1f).padding(start = if (refs.isEmpty()) 0.dp else 4.dp),
                style = MoruTheme.typography.body.copy(fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (layout.showAuthor) {
            MoruText(
                text = commit.authorName,
                modifier = Modifier.width(GitAuthorColumnWidth).padding(end = 8.dp),
                style = MoruTheme.typography.caption,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoruText(
            text = age,
            modifier = Modifier.width(GitTimeColumnWidth).padding(end = 8.dp),
            style = MoruTheme.typography.caption,
            color = secondary,
            maxLines = 1,
        )
        if (layout.showHash) {
            MoruText(
                text = commit.shortHash,
                modifier = Modifier.width(GitHashColumnWidth).padding(end = 12.dp),
                style = MoruTheme.typography.monospace.copy(fontSize = MoruTheme.typography.caption.fontSize, textAlign = TextAlign.End),
                color = secondary,
                maxLines = 1,
            )
        }
    }
}

private fun GitRef.Kind.displayPriority(): Int =
    when (this) {
        GitRef.Kind.Head -> 0
        GitRef.Kind.Worktree -> 1
        GitRef.Kind.LocalBranch -> 2
        GitRef.Kind.Tag -> 3
        GitRef.Kind.RemoteBranch -> 4
    }
