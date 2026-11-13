package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The split both [MoruResizableSplitPane] forms share. The right width is asked for and reported against the width
 * the split has, and every step (a drag or an arrow key) moves from the last width it set, so events that arrive
 * before the next frame add up instead of repeating one step.
 */
@Composable
internal fun ResizableSplitLayout(
    rightWidth: (available: Dp) -> Dp,
    onRightWidthChange: (width: Dp, available: Dp) -> Unit,
    onReset: () -> Unit,
    dividerLabel: String,
    resetLabel: String,
    minLeftWidth: Dp,
    minRightWidth: Dp,
    modifier: Modifier,
    resizable: Boolean,
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
    dividerOverlayModifier: Modifier? = null,
) {
    BoxWithConstraints(modifier) {
        val available = maxWidth
        val dividerWidth = 8.dp
        val hasDividerLane = resizable && dividerOverlayModifier == null
        val room = (available - if (hasDividerLane) dividerWidth else 0.dp).coerceAtLeast(0.dp)
        val minimumTotal = minLeftWidth + minRightWidth
        val scale = if (minimumTotal > 0.dp) minOf(1f, room / minimumTotal) else 1f
        val minimum = minRightWidth * scale
        val maximum = (room - minLeftWidth * scale).coerceAtLeast(minimum)
        val fitted = rightWidth(available).coerceIn(minimum, maximum)
        val cursor = remember { WidthCursor() }
        SideEffect { cursor.width = fitted }
        val moveBy = { delta: Dp ->
            cursor.width = (cursor.width - delta).coerceIn(minimum, maximum)
            onRightWidthChange(cursor.width, available)
        }
        Row(Modifier.fillMaxSize()) {
            left(Modifier.weight(1f).fillMaxHeight())
            if (hasDividerLane) MoruPaneDivider(
                label = dividerLabel,
                resetLabel = resetLabel,
                onAdjust = moveBy,
                onDragDelta = moveBy,
                onReset = onReset,
                modifier = Modifier.width(dividerWidth).fillMaxHeight(),
            )
            right(Modifier.width(fitted).fillMaxHeight())
        }
        if (resizable && dividerOverlayModifier != null) {
            MoruPaneDivider(
                label = dividerLabel,
                resetLabel = resetLabel,
                onAdjust = moveBy,
                onDragDelta = moveBy,
                onReset = onReset,
                // Centered on the seam, so neither pane loses more than half the target to it.
                modifier = Modifier.align(Alignment.TopEnd).offset(x = -fitted + dividerWidth / 2)
                    .width(dividerWidth).then(dividerOverlayModifier),
                overlay = true,
            )
        }
    }
}

private class WidthCursor {
    var width = 0.dp
}
