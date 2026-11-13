package app.thdev.moru.core.designsystem.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Aligns beside the anchor while keeping the entire compact popup inside its Compose owner. With [alignEnd] the popup's
 * end edge meets the anchor's first, so a popup dropping from a button at a pane's end stays over that pane.
 */
internal class MoruPopoverPositionProvider(
    private val drawingInset: Int = 0,
    private val alignEnd: Boolean = false,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val lastX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val lastY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val firstX = drawingInset.coerceIn(0, lastX / 2)
        val firstY = drawingInset.coerceIn(0, lastY / 2)
        val maxX = lastX - firstX
        val maxY = lastY - firstY
        val leading = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left else anchorBounds.right - popupContentSize.width
        val trailing = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val start = if (alignEnd) trailing else leading
        val end = if (alignEnd) leading else trailing
        val x = when {
            start in firstX..maxX -> start
            end in firstX..maxX -> end
            else -> start.coerceIn(firstX, maxX)
        }
        val above = anchorBounds.top - popupContentSize.height
        val y = when {
            anchorBounds.bottom in firstY..maxY -> anchorBounds.bottom
            above in firstY..maxY -> above
            else -> anchorBounds.bottom.coerceIn(firstY, maxY)
        }
        return IntOffset(x, y)
    }
}
