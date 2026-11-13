package app.thdev.moru.feature.git.history

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.feature.git.i18n.historyStrings

/** The names of the commit list's columns, lined up with the cells [GitCommitRowView] draws for the same [layout]. */
@Composable
internal fun GitHistoryHeader(layout: GitRowLayout, modifier: Modifier = Modifier) {
    val strings = historyStrings()
    val style = MoruTheme.typography.caption
    val color = MoruTheme.colors.textSecondary
    Row(
        modifier = modifier.fillMaxWidth().height(24.dp).ckSurface(MoruSurfaceRole.Panel),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoruText(strings.columnGraph, Modifier.width(layout.graphWidth + 6.dp).testTag("git-graph-column").padding(start = 8.dp), style = style,
            color = color, maxLines = 1)
        MoruText(strings.columnDescription, Modifier.weight(1f).padding(start = 8.dp, end = 12.dp), style = style,
            color = color, maxLines = 1)
        if (layout.showAuthor) {
            MoruText(strings.columnAuthor, Modifier.width(GitAuthorColumnWidth).padding(end = 8.dp), style = style,
                color = color, maxLines = 1)
        }
        MoruText(strings.columnDate, Modifier.width(GitTimeColumnWidth).padding(end = 8.dp), style = style,
            color = color, maxLines = 1)
        if (layout.showHash) {
            MoruText(strings.columnCommit, Modifier.width(GitHashColumnWidth).padding(end = 12.dp),
                style = style.copy(textAlign = TextAlign.End), color = color, maxLines = 1)
        }
    }
}
