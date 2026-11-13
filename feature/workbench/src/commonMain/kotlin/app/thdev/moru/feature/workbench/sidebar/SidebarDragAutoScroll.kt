package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest

/** Keeps long lists reachable while the pointer stays near the sidebar's top or bottom edge. */
@Composable
internal fun SidebarDragAutoScroll(state: SidebarDragState, scroll: ScrollState) {
    val density = LocalDensity.current
    val edge = with(density) { 32.dp.toPx() }
    val maximum = with(density) { 10.dp.toPx() }
    LaunchedEffect(state, scroll, edge, maximum) {
        snapshotFlow {
            state.scrollStep(edge, maximum).takeIf {
                (it > 0f && scroll.canScrollForward) || (it < 0f && scroll.canScrollBackward)
            } ?: 0f
        }.collectLatest { step ->
            while (isActive && step != 0f) {
                withFrameNanos { }
                scroll.scrollBy(step)
            }
        }
    }
}
