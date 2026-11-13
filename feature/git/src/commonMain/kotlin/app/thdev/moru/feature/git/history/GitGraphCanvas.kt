package app.thdev.moru.feature.git.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LineWidth = 2.dp
private val NodeRadius = 4.5.dp
private val HaloRadius = 7.dp

/**
 * The graph cell of one commit row: straight vertical lines for lanes passing through,
 * smooth S-curves where a lane joins or leaves the node, and the node itself with a halo
 * in [haloColor] (the row background) so lines never run into it. [laneCount] is the
 * widest row of the whole graph and [width] the cell's width, the same for every row; lanes are
 * spaced to fit it, so a graph with many lanes is compressed instead of crowding out the subject.
 */
@Composable
internal fun GitGraphCanvas(
    row: GitGraphRow,
    laneColors: List<Color>,
    laneCount: Int,
    width: Dp,
    haloColor: Color,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.width(width).fillMaxHeight()) {
        val height = size.height
        val middle = height / 2f
        val laneWidth = size.width / laneCount.coerceAtLeast(1)
        val stroke = Stroke(width = LineWidth.toPx())
        fun laneX(lane: Int) = laneWidth * (lane + 0.5f)
        fun colorOf(index: Int) = laneColors[index.mod(laneColors.size)]

        row.through.forEach { lane ->
            drawLink(laneX(lane.lane), 0f, laneX(lane.lane), height, colorOf(lane.color), stroke)
        }
        row.incoming.forEach { link ->
            drawLink(laneX(link.fromLane), 0f, laneX(link.toLane), middle, colorOf(link.color), stroke)
        }
        row.outgoing.forEach { link ->
            drawLink(laneX(link.fromLane), middle, laneX(link.toLane), height, colorOf(link.color), stroke)
        }

        val center = Offset(laneX(row.nodeLane), middle)
        val nodeColor = colorOf(row.nodeColor)
        drawCircle(haloColor, radius = HaloRadius.toPx(), center = center)
        if (isCurrent) {
            drawCircle(nodeColor, radius = HaloRadius.toPx(), center = center, style = stroke)
        }
        drawCircle(nodeColor, radius = NodeRadius.toPx(), center = center)
    }
}

/** A straight line within one lane, or an S-curve that ends vertical at both ends. */
private fun DrawScope.drawLink(
    fromX: Float,
    fromY: Float,
    toX: Float,
    toY: Float,
    color: Color,
    stroke: Stroke,
) {
    if (fromX == toX) {
        drawLine(color, Offset(fromX, fromY), Offset(toX, toY), strokeWidth = stroke.width)
        return
    }
    val bend = (fromY + toY) / 2f
    val path = Path().apply {
        moveTo(fromX, fromY)
        cubicTo(fromX, bend, toX, bend, toX, toY)
    }
    drawPath(path, color, style = stroke)
}
