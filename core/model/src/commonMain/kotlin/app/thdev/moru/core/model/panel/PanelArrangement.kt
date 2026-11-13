package app.thdev.moru.core.model.panel

/**
 * Free arrangement of a panel tree: items are put beside another item on any side, new items go into the largest
 * place, and a removed item leaves the rest where they were.
 */
object PanelArrangement {
    /**
     * The tree for the items [current], given the [saved] tree and the items it was made for ([previous], null when
     * not known, as after a relaunch). Items that went away are taken out, the rest keep their places, and new items
     * go into the largest place. Without a usable saved tree, [fallback] lays out the items.
     */
    fun reconcile(
        saved: PanelNode?,
        previous: List<String>?,
        current: List<String>,
        fallback: (count: Int) -> PanelNode,
    ): PanelNode {
        if (current.isEmpty()) return fallback(0)
        val usable = saved?.takeIf(::sound)
        if (usable == null || previous == null) {
            return usable?.takeIf { PanelLayouts.slots(it).sorted() == current.indices.toList() } ?: fallback(current.size)
        }
        if (PanelLayouts.slots(usable).sorted() != previous.indices.toList()) return fallback(current.size)
        var tree: PanelNode? = usable
        previous.forEachIndexed { index, id -> if (id !in current) tree = tree?.let { remove(it, index) } }
        val kept = previous.filter { it in current }
        tree = tree?.let { node -> renumber(node) { old -> current.indexOf(previous[old]) } }
        current.forEachIndexed { index, id ->
            if (id !in kept) tree = tree?.let { insertLargest(it, index) } ?: PanelSlot(index)
        }
        return tree ?: fallback(current.size)
    }

    /** The tree with [item] taken out of where it is and put on [edge] of [target]. */
    fun move(node: PanelNode, item: Int, target: Int, edge: PanelEdge): PanelNode {
        if (item == target) return node
        val without = remove(node, item) ?: return node
        return insert(without, item, target, edge)
    }

    /** The tree with [item] put on [edge] of [target], the two sharing what [target] had. */
    fun insert(node: PanelNode, item: Int, target: Int, edge: PanelEdge): PanelNode = when (node) {
        is PanelSlot -> if (node.index == target) pair(node, PanelSlot(item), edge) else node
        is PanelSplit -> {
            val at = node.children.indexOfFirst { it is PanelSlot && it.index == target }
            if (at >= 0 && node.axis == edge.axis) {
                // Beside a sibling along the same line: one more child there, halving the target's share.
                val half = node.shares[at] / 2
                val position = if (edge.before) at else at + 1
                node.copy(
                    children = node.children.toMutableList().apply { add(position, PanelSlot(item)) },
                    shares = node.shares.mapIndexed { i, s -> if (i == at) half else s }.toMutableList()
                        .apply { add(position, half) },
                )
            } else {
                node.copy(children = node.children.map { insert(it, item, target, edge) })
            }
        }
    }

    /** The tree with [item] in the largest place, split along its longer side, for a [width] by [height] area. */
    fun insertLargest(node: PanelNode, item: Int, width: Float = 16f, height: Float = 10f): PanelNode {
        val (target, size) = places(node, width, height).maxBy { (_, size) -> size.first * size.second }
        return insert(node, item, target, if (size.first >= size.second) PanelEdge.Right else PanelEdge.Bottom)
    }

    /** The tree without [item]; its neighbours share its room. Null when it was the only item. */
    fun remove(node: PanelNode, item: Int): PanelNode? = when (node) {
        is PanelSlot -> if (node.index == item) null else node
        is PanelSplit -> {
            removeVerticallyFromColumn(node, item) ?: run {
                val kept = node.children.indices.mapNotNull { i -> remove(node.children[i], item)?.let { it to node.shares[i] } }
                when (kept.size) {
                    0 -> null
                    1 -> kept.single().first
                    else -> {
                        val total = kept.sumOf { it.second.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
                        node.copy(children = kept.map { it.first }, shares = kept.map { it.second / total })
                    }
                }
            }
        }
    }

    /**
     * When a vertical [PanelAxis.Column] contains horizontal [PanelAxis.Row] splits, closing a left or right item in a
     * row should grow its vertical neighbour in the same column rather than stretching the surviving sibling across the
     * whole width.
     */
    private fun removeVerticallyFromColumn(node: PanelSplit, item: Int): PanelNode? {
        if (node.axis != PanelAxis.Column || node.children.size != 2) return null
        val first = node.children[0]
        val second = node.children[1]
        if (first is PanelSplit && first.axis == PanelAxis.Row &&
            second is PanelSplit && second.axis == PanelAxis.Row &&
            first.children.size == second.children.size &&
            (first.children.any { it == PanelSlot(item) } || second.children.any { it == PanelSlot(item) })
        ) {
            val shares = if (first.children.any { it == PanelSlot(item) }) first.shares else second.shares
            val transposed = PanelSplit(
                PanelAxis.Row,
                first.children.indices.map { col ->
                    PanelSplit(PanelAxis.Column, listOf(first.children[col], second.children[col]), node.shares)
                },
                shares,
            )
            return remove(transposed, item)
        }
        if (first is PanelSplit && first.axis == PanelAxis.Row && first.children.size == 2) {
            if (first.children[0] == PanelSlot(item)) {
                return PanelSplit(PanelAxis.Row, listOf(second, first.children[1]), first.shares)
            }
            if (first.children[1] == PanelSlot(item)) {
                return PanelSplit(PanelAxis.Row, listOf(first.children[0], second), first.shares)
            }
        }
        if (second is PanelSplit && second.axis == PanelAxis.Row && second.children.size == 2) {
            if (second.children[0] == PanelSlot(item)) {
                return PanelSplit(PanelAxis.Row, listOf(first, second.children[1]), second.shares)
            }
            if (second.children[1] == PanelSlot(item)) {
                return PanelSplit(PanelAxis.Row, listOf(second.children[0], first), second.shares)
            }
        }
        return null
    }

    private fun renumber(node: PanelNode, to: (Int) -> Int): PanelNode = when (node) {
        is PanelSlot -> PanelSlot(to(node.index))
        is PanelSplit -> node.copy(children = node.children.map { renumber(it, to) })
    }

    private fun pair(existing: PanelNode, added: PanelNode, edge: PanelEdge): PanelNode =
        PanelSplit(edge.axis, if (edge.before) listOf(added, existing) else listOf(existing, added))

    /** Each item with the width and height of its place. */
    private fun places(node: PanelNode, width: Float, height: Float): List<Pair<Int, Pair<Float, Float>>> = when (node) {
        is PanelSlot -> listOf(node.index to (width to height))
        is PanelSplit -> {
            val total = node.shares.sum().takeIf { it > 0f } ?: 1f
            node.children.indices.flatMap { i ->
                val share = node.shares[i] / total
                if (node.axis == PanelAxis.Row) places(node.children[i], width * share, height)
                else places(node.children[i], width, height * share)
            }
        }
    }

    private fun sound(node: PanelNode): Boolean = when (node) {
        is PanelSlot -> node.index >= 0
        is PanelSplit -> node.children.size >= 2 && node.shares.size == node.children.size &&
            node.shares.all { it.isFinite() && it >= 0f } && node.shares.sum() > 0f && node.children.all(::sound)
    }
}
