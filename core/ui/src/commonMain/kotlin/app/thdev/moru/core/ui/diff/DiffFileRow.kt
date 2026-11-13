package app.thdev.moru.core.ui.diff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
/** One changed file in the list, on one line: a status letter, the name, its folder, and the +/- counts. */
@Composable
fun DiffFileRow(file: DiffFile, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val path = file.displayPath
    val name = path.substringAfterLast('/')
    val folder = path.substringBeforeLast('/', missingDelimiterValue = "")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .ckSurface(if (selected) MoruSurfaceRole.CardSelected else MoruSurfaceRole.Page)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(
            text = file.status.letter(),
            style = MoruTheme.typography.monospace.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            color = file.status.color(),
            modifier = Modifier.width(14.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruText(
                text = name,
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (folder.isNotEmpty()) {
                MoruText(
                    text = folder,
                    style = MoruTheme.typography.caption,
                    color = MoruTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        if (file.additions > 0 || file.deletions > 0) {
            MoruText(
                text = "+${file.additions} −${file.deletions}",
                style = MoruTheme.typography.caption,
                color = MoruTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

private fun DiffFileStatus.letter(): String =
    when (this) {
        DiffFileStatus.Modified -> "M"
        DiffFileStatus.Added -> "A"
        DiffFileStatus.Deleted -> "D"
        DiffFileStatus.Renamed -> "R"
    }

@Composable
private fun DiffFileStatus.color(): Color =
    when (this) {
        DiffFileStatus.Added -> MoruTheme.colors.success
        DiffFileStatus.Deleted -> MoruTheme.colors.error
        DiffFileStatus.Modified -> MoruTheme.colors.warning
        DiffFileStatus.Renamed -> MoruTheme.colors.primary
    }
