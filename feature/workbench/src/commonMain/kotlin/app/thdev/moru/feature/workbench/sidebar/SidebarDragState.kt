package app.thdev.moru.feature.workbench.sidebar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** A null Project identifies the Project list; otherwise this is that Project's Task list. */
internal data class SidebarDragScope(val projectId: String? = null)

internal data class SidebarDragItem(val scope: SidebarDragScope, val id: String)

internal data class SidebarDragInsertion(val item: SidebarDragItem, val after: Boolean)

private data class SidebarDragSession(
    val item: SidebarDragItem,
    val ids: List<String>,
    val pointer: Offset,
)

/** Presentation-only drag state. The host receives an entire sibling order only after a valid drop. */
internal class SidebarDragState(private val onReorder: (SidebarDragScope, List<String>) -> Unit) {
    private var siblings: Map<SidebarDragScope, List<String>> = emptyMap()
    private val bounds = mutableMapOf<SidebarDragItem, Rect>()
    private var viewport: Rect? = null
    private var session by mutableStateOf<SidebarDragSession?>(null)
    var insertion by mutableStateOf<SidebarDragInsertion?>(null)
        private set

    val isDragging: Boolean get() = session != null

    fun configure(siblings: Map<SidebarDragScope, List<String>>) {
        this.siblings = siblings
        session?.let { if (siblings[it.item.scope] != it.ids) cancel() }
    }

    fun setViewport(rect: Rect) {
        viewport = rect
        updateInsertion()
    }

    fun setBounds(item: SidebarDragItem, rect: Rect) {
        bounds[item] = rect
        updateInsertion()
    }

    fun removeBounds(item: SidebarDragItem) {
        bounds.remove(item)
        if (session?.item == item) cancel() else updateInsertion()
    }

    fun start(item: SidebarDragItem, pointer: Offset) {
        val ids = siblings[item.scope] ?: return
        if (ids.size < 2 || ids.distinct().size != ids.size || item.id !in ids) return
        session = SidebarDragSession(item, ids, pointer)
        updateInsertion()
    }

    fun drag(pointer: Offset) {
        session = session?.copy(pointer = pointer)
        updateInsertion()
    }

    fun finish() {
        val active = session ?: return
        val target = targetAt(active)
        cancel()
        if (target != null) reordered(active, target).takeIf { it != active.ids }?.let { onReorder(active.item.scope, it) }
    }

    fun cancel() {
        session = null
        insertion = null
    }

    fun isSource(item: SidebarDragItem): Boolean = session?.item == item

    fun move(item: SidebarDragItem, offset: Int): Boolean {
        cancel()
        val ids = siblings[item.scope] ?: return false
        val from = ids.indexOf(item.id)
        val to = from + offset
        if (ids.distinct().size != ids.size || from < 0 || to !in ids.indices) return false
        val reordered = ids.toMutableList().apply { add(to, removeAt(from)) }
        onReorder(item.scope, reordered)
        return true
    }

    /** Pixel step per frame near a viewport edge; never scroll after leaving the sidebar. */
    fun scrollStep(edge: Float, maximum: Float): Float {
        val pointer = session?.pointer ?: return 0f
        val rect = viewport ?: return 0f
        if (!rect.contains(pointer)) return 0f
        return when {
            pointer.y < rect.top + edge -> -maximum * (1f - (pointer.y - rect.top) / edge)
            pointer.y > rect.bottom - edge -> maximum * (1f - (rect.bottom - pointer.y) / edge)
            else -> 0f
        }
    }

    private fun updateInsertion() {
        val active = session
        insertion = active?.let { targetAt(it)?.takeIf { target -> reordered(it, target) != it.ids } }
    }

    private fun targetAt(active: SidebarDragSession): SidebarDragInsertion? {
        if (viewport?.contains(active.pointer) != true) return null
        val rows = active.ids.map { id -> bounds[SidebarDragItem(active.item.scope, id)] ?: return null }
        // The sibling envelope excludes other Projects, headers, footer controls and outside drops.
        val envelope = Rect(rows.minOf { it.left }, rows.first().top, rows.maxOf { it.right }, rows.last().bottom)
        if (!envelope.contains(active.pointer)) return null
        val before = rows.indexOfFirst { active.pointer.y < it.center.y }
        return if (before >= 0) SidebarDragInsertion(SidebarDragItem(active.item.scope, active.ids[before]), after = false)
        else SidebarDragInsertion(SidebarDragItem(active.item.scope, active.ids.last()), after = true)
    }

    private fun reordered(active: SidebarDragSession, target: SidebarDragInsertion): List<String> {
        val from = active.ids.indexOf(active.item.id)
        val boundary = active.ids.indexOf(target.item.id) + if (target.after) 1 else 0
        return active.ids.toMutableList().apply {
            removeAt(from)
            add(if (boundary > from) boundary - 1 else boundary, active.item.id)
        }
    }
}
