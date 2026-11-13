package app.thdev.moru.feature.workbench

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Selection reveals only its row; status changes and manual scrolling do not repeat the request. */
@Composable
internal fun Modifier.revealSidebarSelection(selected: Boolean, enabled: Boolean = true): Modifier {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if (selected && enabled) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    // Enabling again after a Project reveal must not scroll back to its already selected Task or session.
    return bringIntoViewRequester(requester)
}

/** Project navigation places the header near the viewport's top, with room to read its identity and Tasks. */
@Composable
internal fun Modifier.revealSidebarProject(
    requested: Boolean,
    scroll: ScrollState,
    viewportTop: Float,
    onHandled: () -> Unit,
): Modifier {
    var headerTop by remember { mutableFloatStateOf(0f) }
    val handle by rememberUpdatedState(onHandled)
    val viewport by rememberUpdatedState(viewportTop)
    val gap = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(requested) {
        if (requested) {
            withFrameNanos { }
            scroll.animateScrollTo((scroll.value + headerTop - viewport - gap).roundToInt().coerceAtLeast(0))
            handle()
        }
    }
    return onGloballyPositioned { headerTop = it.positionInRoot().y }
}
