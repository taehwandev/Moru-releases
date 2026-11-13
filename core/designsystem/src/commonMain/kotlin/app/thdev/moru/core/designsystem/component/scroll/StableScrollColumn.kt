package app.thdev.moru.core.designsystem.component.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/**
 * A normal vertical ScrollState surface that keeps a visible identity at the same screen position
 * when content above it changes. Native scroll, accessibility and bring-into-view behavior are retained.
 */
@Composable
fun StableScrollColumn(scroll: ScrollState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val registry = remember(scroll) { ScrollAnchorRegistry() }
    Layout(
        content = { CompositionLocalProvider(ScrollAnchorRegistry.Local provides registry, content = content) },
        modifier = modifier.preserveAnchor(scroll, registry).verticalScroll(scroll),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        registry.measure(placeables)
        val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(placeables.sumOf { it.height })
        layout(width, height) {
            var top = 0
            placeables.forEach { placeable -> placeable.placeRelative(0, top); top += placeable.height }
        }
    }
}

private fun Modifier.preserveAnchor(scroll: ScrollState, registry: ScrollAnchorRegistry): Modifier = layout { measurable, constraints ->
    val previous = registry.positions
    val position = scroll.value
    val scrolling = scroll.isScrollInProgress
    // verticalScroll updates its bounds during measurement and reads its offset during placement.
    // Correct between those phases, including at the bottom of a list that just grew or shrank.
    val placeable = measurable.measure(constraints)
    if (position > 0 && !scrolling && previous.isNotEmpty()) {
        val target = (position + registry.anchorDelta(previous, position, placeable.height)).coerceIn(0, scroll.maxValue)
        if (target != scroll.value) scroll.dispatchRawDelta((target - scroll.value).toFloat())
    }
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}
