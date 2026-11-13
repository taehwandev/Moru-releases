package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities

@Composable
internal actual fun NativeDividerTarget(
    onDragDelta: (Dp) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
    vertical: Boolean,
) {
    // Windowless scenes have no interop container; their Compose handle already receives the pointer.
    if (LocalMoruPopupOwner.current == null) return
    val latestDrag by rememberUpdatedState(onDragDelta)
    val latestReset by rememberUpdatedState(onReset)
    // Placed after both panes, so the interop container stacks it above their native content.
    SwingPanel(
        background = Color.Transparent,
        modifier = modifier,
        factory = { NativeDividerComponent({ latestDrag(it) }, { latestReset() }, vertical) },
    )
}

internal class NativeDividerComponent(
    onDrag: (Dp) -> Unit,
    onReset: () -> Unit,
    vertical: Boolean = false,
) : JComponent() {
    init {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(if (vertical) Cursor.N_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR)
        val mouse = object : MouseAdapter() {
            private var last = 0

            private fun along(e: MouseEvent) = if (vertical) e.yOnScreen else e.xOnScreen

            override fun mousePressed(e: MouseEvent) {
                last = along(e)
            }

            // AWT reports logical points, which match dp at Moru's unscaled density.
            override fun mouseDragged(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                val delta = along(e) - last
                last = along(e)
                if (delta != 0) onDrag(delta.dp)
            }

            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) onReset()
            }
        }
        addMouseListener(mouse)
        addMouseMotionListener(mouse)
    }

    // SwingPanel wraps this in an opaque JPanel, which would paint a band over both panes at the seam.
    override fun addNotify() {
        super.addNotify()
        (parent as? JComponent)?.isOpaque = false
    }
}
