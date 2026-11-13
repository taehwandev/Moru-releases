package app.thdev.moru.core.model.panel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PanelArrangementTest {
    private fun row(vararg items: Int) = PanelSplit(PanelAxis.Row, items.map(::PanelSlot))
    private fun column(vararg children: PanelNode) = PanelSplit(PanelAxis.Column, children.toList())

    @Test
    fun anItemPutOnASideOfAnotherSharesThatOnesPlace() {
        // One beside a column of two; the first goes below the third: all three end up stacked.
        val moved = PanelArrangement.move(PanelLayouts.grid(3), 0, 2, PanelEdge.Bottom)
        assertEquals(column(PanelSlot(1), PanelSlot(2), PanelSlot(0)).children, (moved as PanelSplit).children)
        assertEquals(PanelAxis.Column, moved.axis)
        assertEquals(listOf(0.5f, 0.25f, 0.25f), moved.shares)
    }

    @Test
    fun puttingAnItemAcrossTheLineSplitsTheTargetItself() {
        val moved = PanelArrangement.move(row(0, 1, 2), 2, 0, PanelEdge.Top)
        assertEquals(
            PanelSplit(PanelAxis.Row, listOf(column(PanelSlot(2), PanelSlot(0)), PanelSlot(1)), listOf(0.5f, 0.5f)),
            moved,
        )
    }

    @Test
    fun removingAnItemCollapsesASplitLeftWithOneChildAndKeepsTheRestsShares() {
        val tree = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), column(PanelSlot(1), PanelSlot(2)), PanelSlot(3)), listOf(0.2f, 0.5f, 0.3f))
        val without = PanelArrangement.remove(tree, 2) as PanelSplit
        assertEquals(listOf(PanelSlot(0), PanelSlot(1), PanelSlot(3)), without.children)
        assertEquals(listOf(0.2f, 0.5f, 0.3f), without.shares)
        assertNull(PanelArrangement.remove(PanelSlot(0), 0))
    }

    @Test
    fun aNewItemGoesIntoTheLargestPlaceAlongItsLongerSide() {
        // On a wide area one item is split sideways; then the wider of the two halves is split again.
        val two = PanelArrangement.insertLargest(PanelSlot(0), 1)
        assertEquals(row(0, 1), two)
        val three = PanelArrangement.insertLargest(PanelLayouts.resize(two, emptyList(), 0, 0.7f), 2)
        assertEquals(listOf(0, 2, 1), PanelLayouts.slots(three))
    }

    @Test
    fun aClosedTerminalLeavesOnlyItsOwnPlaceAndANewOneTakesTheLargest() {
        val arranged = PanelArrangement.move(PanelLayouts.grid(3), 0, 2, PanelEdge.Bottom)
        // "b" (item 1) closes: the stack keeps "c" above "a", renumbered for the two that remain.
        val closed = PanelArrangement.reconcile(arranged, listOf("a", "b", "c"), listOf("a", "c"), PanelLayouts::grid)
        assertEquals(listOf(1, 0), PanelLayouts.slots(closed))
        assertEquals(PanelAxis.Column, (closed as PanelSplit).axis)
        // "d" opens: it goes into a place of its own without moving the others.
        val opened = PanelArrangement.reconcile(closed, listOf("a", "c"), listOf("a", "c", "d"), PanelLayouts::grid)
        assertEquals(setOf(0, 1, 2), PanelLayouts.slots(opened).toSet())
        assertEquals(listOf(1, 0), PanelLayouts.slots(opened).filter { it != 2 })
    }

    @Test
    fun afterARelaunchASavedTreeIsKeptWhenItHoldsExactlyTheTerminals() {
        val arranged = PanelArrangement.move(PanelLayouts.grid(3), 0, 2, PanelEdge.Bottom)
        assertEquals(arranged, PanelArrangement.reconcile(arranged, null, listOf("x", "y", "z"), PanelLayouts::grid))
        assertEquals(PanelLayouts.grid(2), PanelArrangement.reconcile(arranged, null, listOf("x", "y"), PanelLayouts::grid))
    }

    @Test
    fun moreThanFourStartInTwoRowsForAWideScreen() {
        assertEquals(
            listOf(column(PanelSlot(0), PanelSlot(3)), column(PanelSlot(1), PanelSlot(4)), PanelSlot(2)),
            (PanelLayouts.grid(5) as PanelSplit).children,
        )
        assertEquals(listOf(2, 2, 2, 2), (PanelLayouts.grid(8) as PanelSplit).children.map { (it as PanelSplit).children.size })
    }

    @Test
    fun closingTopLeftOrTopRightGrowsVerticallyAlongTheColumn() {
        // In a 4-pane grid (or legacy Column-of-Rows grid), closing top-left (0) grows bottom-left (2) vertically.
        assertEquals(
            PanelSplit(PanelAxis.Row, listOf(PanelSlot(2), column(PanelSlot(1), PanelSlot(3)))),
            PanelArrangement.remove(PanelLayouts.grid(4), 0),
        )
        val legacyFour = column(row(0, 1), row(2, 3))
        assertEquals(
            PanelSplit(PanelAxis.Row, listOf(PanelSlot(2), column(PanelSlot(1), PanelSlot(3)))),
            PanelArrangement.remove(legacyFour, 0),
        )
        assertEquals(
            PanelSplit(PanelAxis.Row, listOf(column(PanelSlot(0), PanelSlot(2)), PanelSlot(3))),
            PanelArrangement.remove(legacyFour, 1),
        )
        // When the top screen is split into left (0) and right (1) above a bottom pane (2), closing either top pane
        // expands the remaining panes vertically into left/right columns rather than stretching horizontally.
        val topSplit = column(row(0, 1), PanelSlot(2))
        assertEquals(row(2, 1), PanelArrangement.remove(topSplit, 0))
        assertEquals(row(0, 2), PanelArrangement.remove(topSplit, 1))
    }
}
