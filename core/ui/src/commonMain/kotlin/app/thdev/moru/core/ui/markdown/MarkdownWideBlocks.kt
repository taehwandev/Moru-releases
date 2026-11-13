package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.search.TextSearchChild

/**
 * A Markdown table as a grid in one rounded frame, the header row shaded. Each column is as wide as what it holds,
 * within the width there is (see [markdownTableWidths]); a table wider than that scrolls sideways.
 */
@Composable
internal fun MarkdownTable(table: MarkdownBlock.Table, onOpenLink: (String) -> Unit, imageBasePath: String?, imageLoader: MarkdownImageLoader?) {
    val body = MoruTheme.typography.body
    val header = body.copy(fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val sides = MoruTheme.spacing.medium * 2
    val stroke = MoruTheme.stroke.thin
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val available = constraints.maxWidth - with(density) { (stroke * 2).roundToPx() }
        val widths = remember(table, available, body, density) {
            val padding = with(density) { sides.roundToPx() }
            markdownTableWidths(
                measureMarkdownTableColumns(table, measurer, body, header, padding, longestWord = with(density) { LongestWord.roundToPx() }),
                available,
                floor = with(density) { NarrowestColumn.roundToPx() },
            )
        }
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.clip(TableShape).border(stroke, MoruTheme.colors.border, TableShape)) {
                TextSearchChild("row:0") {
                    MarkdownTableRow(table.header, widths, table.alignments, header, header = true, ruled = false, onOpenLink, imageBasePath, imageLoader)
                }
                table.rows.forEachIndexed { index, row ->
                    TextSearchChild("row:${index + 1}") {
                    MarkdownTableRow(row, widths, table.alignments, body, header = false, ruled = true, onOpenLink, imageBasePath, imageLoader)
                    }
                }
            }
        }
    }
}

private val TableShape = RoundedCornerShape(8.dp)

/** A word longer than this, such as a long path, may break rather than widen its column past it. */
private val LongestWord = 280.dp

/** The narrowest a column with long words gets in a narrow pane before the table scrolls sideways instead. */
private val NarrowestColumn = 64.dp
