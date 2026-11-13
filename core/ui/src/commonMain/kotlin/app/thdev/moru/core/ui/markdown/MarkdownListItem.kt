package app.thdev.moru.core.ui.markdown

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

private val ListIndent = 18.dp
private val NumberMarkerWidth = 36.dp
private val BulletMarkerWidth = 20.dp

@Composable
internal fun MarkdownListItem(item: MarkdownBlock.ListItem, onOpenLink: (String) -> Unit, imageBasePath: String?, imageLoader: MarkdownImageLoader?) {
    val gap = MoruTheme.spacing.small
    Layout(
        modifier = Modifier.fillMaxWidth().padding(start = ListIndent * item.depth.coerceAtMost(12)),
        content = {
            val marker = when (item.checked) { true -> "☑"; false -> "☐"; null -> item.number?.let { "$it." } ?: "•" }
            MoruText(marker, style = MoruTheme.typography.body, modifier = Modifier.width(if (item.number != null) NumberMarkerWidth else BulletMarkerWidth))
            MarkdownInlineText(item.text, onOpenLink, imageBasePath = imageBasePath, imageLoader = imageLoader)
        },
    ) { measurables, constraints ->
        val marker = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val gapPx = gap.roundToPx()
        val textWidth = if (constraints.hasBoundedWidth) (constraints.maxWidth - marker.width - gapPx).coerceAtLeast(0) else constraints.maxWidth
        val content = measurables[1].measure(constraints.copy(
            minWidth = if (constraints.hasBoundedWidth) textWidth else 0,
            maxWidth = textWidth,
            minHeight = 0,
        ))
        val center = content[MarkdownFirstLineCenter]
        val markerCenter = marker.height / 2
        // An image-only item keeps its original top alignment. Text uses its real first-line
        // geometry even when bold or monospace spans make that line taller than the marker.
        val markerY = if (center == AlignmentLine.Unspecified) 0 else (center - markerCenter).coerceAtLeast(0)
        val contentY = if (center == AlignmentLine.Unspecified) 0 else (markerCenter - center).coerceAtLeast(0)
        val lines: Map<AlignmentLine, Int> = if (center == AlignmentLine.Unspecified) emptyMap() else mapOf(MarkdownFirstLineCenter to center + contentY)
        layout(
            width = constraints.constrainWidth(marker.width + gapPx + content.width),
            height = constraints.constrainHeight(maxOf(markerY + marker.height, contentY + content.height)),
            alignmentLines = lines,
        ) {
            marker.placeRelative(0, markerY)
            content.placeRelative(marker.width + gapPx, contentY)
        }
    }
}
