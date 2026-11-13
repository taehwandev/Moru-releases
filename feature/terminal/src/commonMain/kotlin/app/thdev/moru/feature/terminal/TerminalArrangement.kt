package app.thdev.moru.feature.terminal

import app.thdev.moru.core.model.panel.PanelArrangement
import app.thdev.moru.core.model.panel.PanelEdge
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode

/**
 * Keeps a Task's terminal arrangement while its terminals come and go: it remembers which terminals the last tree
 * was made for, so a closed terminal leaves only its own place and a new one goes into the largest place. A new
 * terminal opened by a split ([placement], see [TerminalSplitRequests]) goes on its side of the terminal it split,
 * and then every terminal is made the same size.
 */
internal class TerminalArrangement(
    private val placement: (newSessionId: String) -> Pair<String, PanelEdge>? = TerminalSplitRequests::take,
) {
    private var ids: List<String>? = null
    private var tree: PanelNode? = null

    /** The tree for the terminals [current], starting from [saved] when it was made for the terminals last seen. */
    fun resolve(saved: PanelNode?, current: List<String>): PanelNode {
        val last = ids
        // The saved tree lags one frame behind a change this pane reported, so the pane's own tree wins until then.
        val base = saved?.takeIf { last == null || fits(it, last.size) } ?: tree ?: saved
        var next = PanelArrangement.reconcile(base, last, current, PanelLayouts::grid)
        if (last != null) {
            current.filter { it !in last }.forEach { id ->
                val (beside, edge) = placement(id) ?: return@forEach
                val target = current.indexOf(beside).takeIf { it >= 0 } ?: return@forEach
                next = PanelLayouts.equalize(PanelArrangement.move(next, current.indexOf(id), target, edge))
            }
        }
        ids = current
        tree = next
        return next
    }

    private fun fits(node: PanelNode, count: Int) = PanelLayouts.slots(node).sorted() == (0 until count).toList()
}
