package app.thdev.moru.core.model.panel

/** The shapes a host starts from, and the rules for keeping a saved tree. */
object PanelLayouts {
    /**
     * Up to four items as the terminal grid always drew them: one alone, two side by side, three as one beside a
     * column of two, four as two columns of two. More than four are columns of up to two items side by side, which
     * suits a wide screen while keeping each column stacked vertically when a pane closes.
     */
    fun grid(count: Int): PanelNode = when {
        count <= 1 -> PanelSlot(0)
        count == 2 -> PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1)))
        count == 3 -> PanelSplit(
            PanelAxis.Row,
            listOf(PanelSlot(0), PanelSplit(PanelAxis.Column, listOf(PanelSlot(1), PanelSlot(2)))),
        )
        else -> {
            val columns = (count + 1) / 2
            PanelSplit(
                PanelAxis.Row,
                (0 until columns).map { col ->
                    if (col + columns < count) {
                        PanelSplit(PanelAxis.Column, listOf(PanelSlot(col), PanelSlot(col + columns)))
                    } else {
                        PanelSlot(col)
                    }
                },
            )
        }
    }

    /** [count] items side by side in even shares; one item alone. */
    fun row(count: Int): PanelNode =
        if (count <= 1) PanelSlot(0) else PanelSplit(PanelAxis.Row, (0 until count).map(::PanelSlot))

    /**
     * [saved] when it has the same shape as [fallback], sound shares and the same items in some order (the user may
     * have moved them), otherwise [fallback].
     */
    fun keepIfSameShape(saved: PanelNode?, fallback: PanelNode): PanelNode =
        if (saved != null && sameShape(saved, fallback) && soundShares(saved) &&
            slots(saved).sorted() == slots(fallback).sorted()
        ) saved else fallback

    /** Whether [a] and [b] have the same splits and axes with a slot in the same places, whatever their shares. */
    fun sameShape(a: PanelNode, b: PanelNode): Boolean = when {
        a is PanelSlot && b is PanelSlot -> true
        a is PanelSplit && b is PanelSplit -> a.axis == b.axis && a.children.size == b.children.size &&
            a.children.indices.all { i -> sameShape(a.children[i], b.children[i]) }
        else -> false
    }

    /** The tree with the places of items [a] and [b] exchanged; sizes stay where they were. */
    fun swap(node: PanelNode, a: Int, b: Int): PanelNode = when (node) {
        is PanelSlot -> when (node.index) {
            a -> PanelSlot(b)
            b -> PanelSlot(a)
            else -> node
        }
        is PanelSplit -> node.copy(children = node.children.map { swap(it, a, b) })
    }

    /**
     * The tree with every item the same size: each split shares its room by how many items its children lay along
     * its line, so a column of two beside one item gets the same width as that item, not half of it.
     */
    fun equalize(node: PanelNode): PanelNode = when (node) {
        is PanelSlot -> node
        is PanelSplit -> {
            val children = node.children.map(::equalize)
            val spans = children.map { span(it, node.axis).toFloat() }
            val total = spans.sum()
            node.copy(children = children, shares = spans.map { it / total })
        }
    }

    private fun span(node: PanelNode, axis: PanelAxis): Int = when (node) {
        is PanelSlot -> 1
        is PanelSplit -> if (node.axis == axis) node.children.sumOf { span(it, axis) } else node.children.maxOf { span(it, axis) }
    }

    /** The items of [node] in the order they are laid out. */
    fun slots(node: PanelNode): List<Int> = when (node) {
        is PanelSlot -> listOf(node.index)
        is PanelSplit -> node.children.flatMap(::slots)
    }

    /**
     * The tree with the seam after child [seam] of the split at [path] moved so that child takes [share] of the two
     * neighbours' room. [path] is the child index at each level from the root.
     */
    fun resize(node: PanelNode, path: List<Int>, seam: Int, share: Float): PanelNode {
        if (node !is PanelSplit) return node
        if (path.isNotEmpty()) {
            val child = path.first()
            if (child !in node.children.indices) return node
            return node.copy(
                children = node.children.mapIndexed { i, c -> if (i == child) resize(c, path.drop(1), seam, share) else c },
            )
        }
        if (seam !in 0 until node.children.size - 1) return node
        val pair = node.shares[seam] + node.shares[seam + 1]
        val left = (share.coerceIn(0f, 1f) * pair)
        return node.copy(shares = node.shares.mapIndexed { i, s ->
            when (i) {
                seam -> left
                seam + 1 -> pair - left
                else -> s
            }
        })
    }

    /** The tree with the seam after child [seam] of the split at [path] back to an even split of its two neighbours. */
    fun even(node: PanelNode, path: List<Int>, seam: Int): PanelNode = resize(node, path, seam, 0.5f)

    private fun soundShares(node: PanelNode): Boolean = when (node) {
        is PanelSlot -> true
        is PanelSplit -> node.shares.size == node.children.size &&
            node.shares.all { it.isFinite() && it >= 0f } &&
            node.shares.sum() > 0f &&
            node.children.all(::soundShares)
    }
}
