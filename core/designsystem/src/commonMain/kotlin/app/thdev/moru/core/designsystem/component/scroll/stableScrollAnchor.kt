package app.thdev.moru.core.designsystem.component.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/** Marks a persistent row inside [StableScrollColumn]; inert outside that scope. */
@Composable
fun Modifier.stableScrollAnchor(key: Any): Modifier {
    val registry = ScrollAnchorRegistry.Local.current ?: return this
    val line = remember(registry, key) { registry.acquire(key) }
    DisposableEffect(registry, key, line) { onDispose { registry.release(key, line) } }
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height, mapOf(line to 0)) { placeable.placeRelative(0, 0) }
    }
}
