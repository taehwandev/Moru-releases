package app.thdev.moru.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Component
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.RootPaneContainer

@Composable
internal actual fun rememberMoruPopoverWindowSize(): DpSize? {
    val owner = LocalMoruPopupOwner.current ?: return null
    val content = (owner as? RootPaneContainer)?.contentPane ?: owner
    var size by remember(owner, content) { mutableStateOf(content.popoverSize()) }
    DisposableEffect(owner, content) {
        val listener = object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) { size = content.popoverSize() }
        }
        content.addComponentListener(listener)
        if (content !== owner) owner.addComponentListener(listener)
        onDispose {
            content.removeComponentListener(listener)
            if (content !== owner) owner.removeComponentListener(listener)
        }
    }
    return size
}

private fun Component.popoverSize(): DpSize? {
    if (width <= 0 || height <= 0) return null
    val edge = if (this is Window) insets else null
    return DpSize(
        (width - (edge?.left ?: 0) - (edge?.right ?: 0)).coerceAtLeast(0).dp,
        (height - (edge?.top ?: 0) - (edge?.bottom ?: 0)).coerceAtLeast(0).dp,
    )
}
