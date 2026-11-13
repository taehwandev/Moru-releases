package app.thdev.moru.core.designsystem.component.scroll

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Placeable

/** Layout-only identities and coordinates; no observable state or background work. */
internal class ScrollAnchorRegistry {
    private class Entry(val line: HorizontalAlignmentLine, var readers: Int = 0)
    private val entries = linkedMapOf<Any, Entry>()
    var positions: Map<Any, Int> = emptyMap()
        private set

    fun acquire(key: Any): HorizontalAlignmentLine =
        entries.getOrPut(key) { Entry(HorizontalAlignmentLine(::minOf)) }.also { it.readers++ }.line

    fun release(key: Any, line: HorizontalAlignmentLine) {
        val entry = entries[key] ?: return
        if (entry.line === line && --entry.readers == 0) entries.remove(key)
    }

    fun measure(placeables: List<Placeable>) {
        positions = buildMap {
            var top = 0
            placeables.forEach { placeable ->
                this@ScrollAnchorRegistry.entries.forEach { (key, entry) ->
                    val offset = placeable[entry.line]
                    if (offset != AlignmentLine.Unspecified) put(key, top + offset)
                }
                top += placeable.height
            }
        }
    }

    fun anchorDelta(previous: Map<Any, Int>, scroll: Int, viewport: Int): Int {
        val surviving = previous.entries.filter { it.key in positions }
        val visible = surviving.filter { it.value >= scroll && it.value < scroll + viewport }.minByOrNull { it.value }
        val anchor = visible ?: surviving.filter { it.value <= scroll }.maxByOrNull { it.value } ?: return 0
        return positions.getValue(anchor.key) - anchor.value
    }

    companion object {
        val Local = staticCompositionLocalOf<ScrollAnchorRegistry?> { null }
    }
}
