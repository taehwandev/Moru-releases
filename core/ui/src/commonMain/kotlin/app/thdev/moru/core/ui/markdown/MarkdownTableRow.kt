package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.search.TextSearchChild

/**
 * One row of a Markdown table: every cell [widths] wide and as tall as the row's tallest, so the grid lines meet. A
 * hairline parts the cells, and one above the row parts it from the row before when [ruled].
 */
@Composable
internal fun MarkdownTableRow(
    cells: List<String>,
    widths: List<Int>,
    alignments: List<TableAlignment>,
    style: TextStyle,
    header: Boolean,
    ruled: Boolean,
    onOpenLink: (String) -> Unit,
    imageBasePath: String?,
    imageLoader: MarkdownImageLoader?,
) {
    val line = MoruTheme.colors.border
    val stroke = MoruTheme.stroke.thin
    val density = LocalDensity.current
    Row(
        Modifier.height(IntrinsicSize.Min)
            .then(if (header) Modifier.ckSurface(MoruSurfaceRole.Code) else Modifier)
            .drawBehind { if (ruled) drawLine(line, Offset.Zero, Offset(size.width, 0f), stroke.toPx()) },
    ) {
        widths.forEachIndexed { index, width ->
            TextSearchChild("cell:$index") {
            MarkdownInlineText(
                text = cells.getOrElse(index) { "" },
                onOpenLink = onOpenLink,
                modifier = Modifier.width(with(density) { width.toDp() }).fillMaxHeight()
                    .drawBehind { if (index > 0) drawLine(line, Offset.Zero, Offset(0f, size.height), stroke.toPx()) }
                    .padding(horizontal = MoruTheme.spacing.medium, vertical = MoruTheme.spacing.small),
                style = style.copy(
                    textAlign = when (alignments.getOrElse(index) { TableAlignment.Start }) {
                        TableAlignment.Start -> TextAlign.Start
                        TableAlignment.Center -> TextAlign.Center
                        TableAlignment.End -> TextAlign.End
                    },
                ),
                imageBasePath = imageBasePath,
                imageLoader = imageLoader,
            )
            }
        }
    }
}
