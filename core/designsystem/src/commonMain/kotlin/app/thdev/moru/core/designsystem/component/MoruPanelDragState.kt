package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Moves panels by dragging one panel's handle (usually its header) onto another panel. Each panel marks its area
 * with [dropTarget] and its handle with [handle]; dropping a handle on another panel reports both keys and [edge]:
 * the middle of a panel, or the side the pointer is nearest when it is within a quarter of the panel from that side.
 * While a handle is dragged, [target] and [edge] name where it would land, so that panel can show it.
 * Hold one with `remember { MoruPanelDragState() }`.
 * Set [allowEdges] to false for hosts that only exchange entire panels.
 */
@Stable
class MoruPanelDragState(private val allowEdges: Boolean = true) {
    private val areas = mutableMapOf<Any, Pair<Any, Rect>>()
    private val handles = mutableMapOf<Any, Offset>()
    private var pointer = Offset.Zero

    /** The panel whose handle is being dragged, or null. */
    var dragging by mutableStateOf<Any?>(null)
        private set

    /** Another panel under the dragged handle, or null. */
    var target by mutableStateOf<Any?>(null)
        private set

    /** Where on [target] the handle would land. */
    var edge by mutableStateOf(MoruDropEdge.Center)
        private set

    /** The area, in the window's coordinates, that the dragged panel would take: [target]'s side or all of it. */
    var landing by mutableStateOf<Rect?>(null)
        private set

    /** Marks the area of panel [key], where another panel's handle can be dropped. */
    fun dropTarget(key: Any): Modifier = Modifier.composed {
        val owner = remember { Any() }
        DisposableEffect(this@MoruPanelDragState, key) {
            onDispose {
                if (areas[key]?.first === owner) {
                    areas.remove(key)
                    handles.remove(key)
                    if (dragging == key) finish()
                    if (target == key) { target = null; landing = null; edge = MoruDropEdge.Center }
                }
            }
        }
        Modifier.onGloballyPositioned { areas[key] = owner to it.boundsInRoot() }
    }

    /** Makes this the handle of panel [key]; dropping it on another panel calls [onMove] with both keys. */
    fun handle(key: Any, onMove: (from: Any, to: Any, edge: MoruDropEdge) -> Unit): Modifier = Modifier.composed {
        val move by rememberUpdatedState(onMove)
        Modifier
        .onGloballyPositioned { handles[key] = it.positionInRoot() }
        .pointerInput(this@MoruPanelDragState, key) {
            detectDragGestures(
                onDragStart = { offset -> start(key, (handles[key] ?: Offset.Zero) + offset) },
                // Where the pointer is, not a sum of steps: the first step after the touch slop repeats part of it.
                onDrag = { change, _ -> change.consume(); moveTo((handles[key] ?: Offset.Zero) + change.position) },
                onDragEnd = {
                    val landing = edge
                    finish()?.let { (from, to) -> move(from, to, landing) }
                },
                onDragCancel = { finish() },
            )
        }
    }

    private fun start(key: Any, at: Offset) {
        dragging = key
        pointer = at
        target = null
    }

    private fun moveTo(at: Offset) {
        pointer = at
        val hit = areas.entries.firstOrNull { (key, area) -> key != dragging && area.second.contains(pointer) }
        target = hit?.key
        edge = if (allowEdges) hit?.value?.second?.let { area -> edgeOf(area, pointer) } ?: MoruDropEdge.Center
            else MoruDropEdge.Center
        landing = hit?.value?.second?.let { area ->
            when (edge) {
                MoruDropEdge.Center -> area
                MoruDropEdge.Left -> area.copy(right = area.left + area.width / 2)
                MoruDropEdge.Right -> area.copy(left = area.left + area.width / 2)
                MoruDropEdge.Top -> area.copy(bottom = area.top + area.height / 2)
                MoruDropEdge.Bottom -> area.copy(top = area.top + area.height / 2)
            }
        }
    }

    private fun edgeOf(area: Rect, at: Offset): MoruDropEdge {
        if (area.width <= 0f || area.height <= 0f) return MoruDropEdge.Center
        val x = (at.x - area.left) / area.width
        val y = (at.y - area.top) / area.height
        val nearest = listOf(MoruDropEdge.Left to x, MoruDropEdge.Right to 1f - x, MoruDropEdge.Top to y, MoruDropEdge.Bottom to 1f - y)
            .minBy { it.second }
        return if (nearest.second < 0.25f) nearest.first else MoruDropEdge.Center
    }

    private fun finish(): Pair<Any, Any>? {
        val move = dragging?.let { from -> target?.let { to -> from to to } }
        dragging = null
        target = null
        edge = MoruDropEdge.Center
        landing = null
        return move
    }
}
