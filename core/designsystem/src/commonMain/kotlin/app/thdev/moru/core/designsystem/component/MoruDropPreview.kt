package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.thdev.moru.core.designsystem.theme.MoruTheme

/**
 * While a panel is dragged, the exact area it would take, tinted and outlined with a large mark for the side
 * (or ⇄ for trading places). It is drawn in a popup so it shows over native panel content such as terminals, takes no
 * focus and lets the drag go on underneath.
 */
@Composable
fun MoruDropPreview(state: MoruPanelDragState) {
    val area = state.landing ?: return
    val density = LocalDensity.current
    val position = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ) = IntOffset(area.left.toInt(), area.top.toInt())
    }
    Popup(popupPositionProvider = position, properties = PopupProperties(focusable = false)) {
        val shape = RoundedCornerShape(MoruTheme.radius.small)
        Box(
            Modifier
                .size(with(density) { area.width.toDp() }, with(density) { area.height.toDp() })
                .padding(3.dp)
                .background(MoruTheme.colors.primary.copy(alpha = .22f), shape)
                .border(2.dp, MoruTheme.colors.primary, shape),
            contentAlignment = Alignment.Center,
        ) {
            MoruText(
                text = when (state.edge) {
                    MoruDropEdge.Center -> "⇄"
                    MoruDropEdge.Left -> "←"
                    MoruDropEdge.Right -> "→"
                    MoruDropEdge.Top -> "↑"
                    MoruDropEdge.Bottom -> "↓"
                },
                color = MoruTheme.colors.primary,
                style = MoruTheme.typography.body.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}
