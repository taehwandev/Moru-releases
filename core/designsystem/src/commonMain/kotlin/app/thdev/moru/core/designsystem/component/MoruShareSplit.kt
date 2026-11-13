package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Children laid side by side, or stacked when [vertical], each taking its share of the room, with a draggable seam
 * between each pair of neighbours. A seam reports the share of its two neighbours' room the first of them takes, so
 * moving one seam never changes another. No child is dragged below [minSize] while the pair has room for two. When the
 * room shrinks so that a child would fall below [minSize], the children are laid out evenly until there is room again;
 * [shares] are kept, not changed.
 * Seams are drawn over the boundary, after the children, so they take the pointer even over native content.
 *
 * The room is read from the laid-out size rather than through a subcomposition: callers keep their children as movable
 * content (terminal panes), and moving that out of a subcomposition being disposed, as when closing a pane collapses
 * the split, crashes Compose.
 */
@Composable
fun MoruShareSplit(
    vertical: Boolean,
    shares: List<Float>,
    onSeamMove: (seam: Int, share: Float) -> Unit,
    onSeamReset: (seam: Int) -> Unit,
    dividerLabel: String,
    resetLabel: String,
    minSize: Dp,
    modifier: Modifier = Modifier,
    child: @Composable (index: Int, Modifier) -> Unit,
) {
    val density = LocalDensity.current
    var room by remember { mutableStateOf<Dp?>(null) }
    Box(modifier.onSizeChanged { size -> room = with(density) { (if (vertical) size.height else size.width).toDp() } }) {
        val total = shares.sum().takeIf { it > 0f } ?: 1f
        // Half a point of slack, so a child dragged to exactly the minimum is not taken as squeezed. Before the first
        // layout the room is unknown, and the kept shares stand.
        val squeezed = room?.let { known -> shares.any { share -> known * (share / total) < minSize - 0.5.dp } } ?: false
        val laidOut = if (squeezed) List(shares.size) { 1f / shares.size } else shares.map { it / total }
        val sizes = laidOut.map { share -> (room ?: 0.dp) * share }
        // Every step (a drag or an arrow key) moves from the last size it set, so steps before the next frame add up.
        val cursors = remember { SeamCursors() }
        SideEffect { cursors.sizes = sizes.toMutableList() }
        if (vertical) {
            Column(Modifier.fillMaxSize()) {
                shares.indices.forEach { i -> child(i, Modifier.weight(weightOf(laidOut[i])).fillMaxWidth()) }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                shares.indices.forEach { i -> child(i, Modifier.weight(weightOf(laidOut[i])).fillMaxHeight()) }
            }
        }
        val seamWidth = 8.dp
        var boundary = 0.dp
        for (seam in 0 until shares.size - 1) {
            boundary += sizes[seam]
            val pair = sizes[seam] + sizes[seam + 1]
            val least = minOf(minSize, pair / 2)
            val moveBy = moveBy@{ delta: Dp ->
                // An even layout stands in for the kept shares while squeezed, so a drag there must not change them.
                if (squeezed) return@moveBy
                val current = cursors.sizes.getOrNull(seam) ?: sizes[seam]
                val next = (current + delta).coerceIn(least, (pair - least).coerceAtLeast(least))
                if (seam < cursors.sizes.size) cursors.sizes[seam] = next
                if (pair > 0.dp) onSeamMove(seam, next / pair)
            }
            val centered = boundary - seamWidth / 2
            MoruPaneDivider(
                label = dividerLabel,
                resetLabel = resetLabel,
                onAdjust = moveBy,
                onDragDelta = moveBy,
                onReset = { onSeamReset(seam) },
                modifier = if (vertical) Modifier.offset(y = centered).fillMaxWidth().height(seamWidth)
                else Modifier.offset(x = centered).fillMaxHeight().width(seamWidth),
                overlay = true,
                vertical = vertical,
            )
        }
    }
}

// A zero weight is rejected by Compose; a vanishing share still keeps its child in the layout.
private fun weightOf(share: Float): Float = share.coerceAtLeast(0.0001f)

private class SeamCursors {
    var sizes: MutableList<Dp> = mutableListOf()
}
