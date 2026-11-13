package app.thdev.moru.feature.workbench

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry

internal val TreeIndent = 14.dp
internal val TreeStart = 8.dp

/** The style of a row's name; the explorer measures names in it to size its sideways scroll. */
@Composable
internal fun projectTreeNameStyle(selected: Boolean = false) =
    MoruTheme.typography.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)

/**
 * One file or folder of the explorer: indent guides, a chevron on folders, an icon and the name. What git ignores is
 * dimmed; a changed file (or a folder holding one) takes the change's color and letter, as the Git box colors it.
 */
@Composable
internal fun ProjectTreeRow(
    entry: WorkbenchProjectTreeEntry,
    expanded: Boolean,
    selected: Boolean,
    change: ProjectTreeChange?,
    onClick: () -> Unit,
    /** Tighter rows, chosen in the explorer's options, to see more of a deep tree at once. */
    compact: Boolean = false,
) {
    val colors = MoruTheme.colors
    val dim = colors.textSecondary.copy(alpha = 0.5f)
    val nameColor = when {
        change != null -> change.color()
        entry.isIgnored -> dim
        selected -> colors.textPrimary
        else -> colors.textPrimary.copy(alpha = 0.88f)
    }
    val iconTint = when {
        entry.isIgnored -> dim
        entry.isFile -> colors.textSecondary
        else -> colors.primary.copy(alpha = 0.85f)
    }
    val guide = colors.border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 20.dp else 24.dp)
            .ckSurface(if (selected) MoruSurfaceRole.RowSelected else MoruSurfaceRole.Row)
            .clickable(onClick = onClick)
            .drawBehind {
                repeat(entry.depth) { level ->
                    val x = (TreeStart + TreeIndent * level + 6.dp).toPx()
                    drawLine(guide, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                }
            }
            .padding(start = TreeStart + TreeIndent * entry.depth, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.isFile) {
            Spacer(Modifier.width(12.dp))
        } else {
            MoruIcon(if (expanded) MoruIconName.ChevronDown else MoruIconName.ChevronRight, size = 12.dp, tint = dim)
        }
        MoruIcon(
            name = when {
                entry.isFile -> MoruIconName.File
                expanded -> MoruIconName.FolderOpen
                else -> MoruIconName.Folder
            },
            size = 14.dp,
            tint = iconTint,
        )
        MoruText(
            text = entry.name,
            modifier = Modifier.weight(1f).padding(start = 2.dp),
            color = nameColor,
            style = projectTreeNameStyle(selected),
            // The explorer is made wide enough for every name and scrolls sideways, so a name is never cut short.
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        if (change != null) {
            MoruText(
                text = if (entry.isFile) change.letter else "•",
                color = change.color(),
                style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProjectTreeChange.color(): Color = when (this) {
    ProjectTreeChange.Untracked -> MoruTheme.colors.primary
    ProjectTreeChange.Modified -> MoruTheme.colors.warning
    ProjectTreeChange.Added -> MoruTheme.colors.success
    ProjectTreeChange.Conflicted -> MoruTheme.colors.error
}
