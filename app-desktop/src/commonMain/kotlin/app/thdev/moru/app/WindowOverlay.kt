package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** A reader above native terminals, bounded by the active terminal workspace when one is registered. */
internal class WindowOverlay {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
    var widthFraction by mutableStateOf<Float?>(null)
    var region by mutableStateOf<Rect?>(null)
        private set
    var hasRegion by mutableStateOf(false)
        private set
    private var regionOwner: Any? = null

    fun claimRegion(owner: Any) { regionOwner = owner; hasRegion = true; region = null }
    fun placeRegion(owner: Any, bounds: Rect) { if (regionOwner === owner) region = bounds }
    fun releaseRegion(owner: Any) {
        if (regionOwner === owner) { regionOwner = null; region = null; hasRegion = false }
    }
}

internal val LocalWindowOverlay = staticCompositionLocalOf<WindowOverlay?> { null }

/** Records the complete terminal workspace, not an individual split session; leaves the Result sidebar outside. */
@Composable
internal fun WindowOverlayRegion(modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    val overlay = LocalWindowOverlay.current
    val owner = remember(overlay) { Any() }
    DisposableEffect(overlay, owner) {
        overlay?.claimRegion(owner)
        onDispose { overlay?.releaseRegion(owner) }
    }
    content(modifier.onGloballyPositioned { overlay?.placeRegion(owner, it.boundsInRoot()) })
}

/**
 * Shows [content] over the registered workspace while this call stays in composition. Without a registered region
 * it uses the window's content area; without a window overlay it is drawn in place.
 */
@Composable
internal fun ShowOverWindow(widthFraction: Float? = null, content: @Composable () -> Unit) {
    val overlay = LocalWindowOverlay.current
    if (overlay == null) {
        content()
        return
    }
    val latest by rememberUpdatedState(content)
    DisposableEffect(overlay, widthFraction) {
        val shown: @Composable () -> Unit = { latest() }
        overlay.widthFraction = widthFraction?.coerceIn(0.35f, 0.8f)
        overlay.content = shown
        onDispose {
            if (overlay.content === shown) { overlay.content = null; overlay.widthFraction = null }
        }
    }
}
