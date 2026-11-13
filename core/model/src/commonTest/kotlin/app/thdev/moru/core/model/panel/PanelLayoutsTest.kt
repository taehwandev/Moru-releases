package app.thdev.moru.core.model.panel

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PanelLayoutsTest {
    @Test
    fun theGridKeepsTheShapesTheFixedTerminalGridDrew() {
        assertEquals(PanelSlot(0), PanelLayouts.grid(1))
        assertEquals(PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1))), PanelLayouts.grid(2))
        assertEquals(
            PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSplit(PanelAxis.Column, listOf(PanelSlot(1), PanelSlot(2))))),
            PanelLayouts.grid(3),
        )
        assertEquals(
            PanelSplit(
                PanelAxis.Row,
                listOf(
                    PanelSplit(PanelAxis.Column, listOf(PanelSlot(0), PanelSlot(2))),
                    PanelSplit(PanelAxis.Column, listOf(PanelSlot(1), PanelSlot(3))),
                ),
            ),
            PanelLayouts.grid(4),
        )
    }

    @Test
    fun aSavedTreeIsKeptOnlyWhileItHasTheSameShape() {
        val saved = PanelLayouts.resize(PanelLayouts.grid(2), emptyList(), 0, 0.7f)
        assertSame(saved, PanelLayouts.keepIfSameShape(saved, PanelLayouts.grid(2)))
        assertEquals(PanelLayouts.grid(3), PanelLayouts.keepIfSameShape(saved, PanelLayouts.grid(3)))
        assertEquals(PanelLayouts.grid(2), PanelLayouts.keepIfSameShape(null, PanelLayouts.grid(2)))
    }

    @Test
    fun brokenSharesFallBackToTheEvenTree() {
        val broken = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1)), listOf(Float.NaN, 0.5f))
        assertEquals(PanelLayouts.grid(2), PanelLayouts.keepIfSameShape(broken, PanelLayouts.grid(2)))
        val missing = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1)), listOf(1f))
        assertEquals(PanelLayouts.grid(2), PanelLayouts.keepIfSameShape(missing, PanelLayouts.grid(2)))
    }

    @Test
    fun resizingANestedSeamLeavesTheOtherSeamsAlone() {
        val resized = PanelLayouts.resize(PanelLayouts.grid(3), listOf(1), 0, 0.25f)
        val outer = resized as PanelSplit
        assertEquals(listOf(0.5f, 0.5f), outer.shares)
        assertEquals(listOf(0.25f, 0.75f), (outer.children[1] as PanelSplit).shares)
        assertEquals(listOf(0.5f, 0.5f), (PanelLayouts.even(resized, listOf(1), 0).let { (it as PanelSplit).children[1] as PanelSplit }).shares)
    }

    @Test
    fun aSeamMovesOnlyTheRoomOfItsTwoNeighbours() {
        val three = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1), PanelSlot(2)), listOf(0.2f, 0.3f, 0.5f))
        val resized = PanelLayouts.resize(three, emptyList(), 1, 0.0f) as PanelSplit
        assertEquals(0.2f, resized.shares[0])
        assertEquals(0f, resized.shares[1], 1e-6f)
        assertEquals(0.8f, resized.shares[2], 1e-6f)
    }

    @Test
    fun aTreeSurvivesTheLayoutFile() {
        val tree: PanelNode = PanelLayouts.resize(PanelLayouts.grid(4), listOf(0), 0, 0.6f)
        val json = Json { encodeDefaults = false }
        assertEquals(tree, json.decodeFromString(PanelNode.serializer(), json.encodeToString(PanelNode.serializer(), tree)))
    }

    @Test
    fun aRowLaysItemsSideBySide() {
        assertEquals(PanelSlot(0), PanelLayouts.row(1))
        assertEquals(PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(1), PanelSlot(2))), PanelLayouts.row(3))
    }

    @Test
    fun swappedItemsStayWhereTheUserMovedThemWithTheirSizes() {
        val sized = PanelLayouts.resize(PanelLayouts.grid(3), listOf(1), 0, 0.3f)
        val swapped = PanelLayouts.swap(sized, 0, 2)
        assertEquals(listOf(2, 1, 0), PanelLayouts.slots(swapped))
        assertEquals(listOf(0.3f, 0.7f), ((swapped as PanelSplit).children[1] as PanelSplit).shares)
        assertSame(swapped, PanelLayouts.keepIfSameShape(swapped, PanelLayouts.grid(3)))
    }

    @Test
    fun aTreeThatLostOrRepeatsAnItemIsNotKept() {
        val repeated = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSlot(0)))
        assertEquals(PanelLayouts.grid(2), PanelLayouts.keepIfSameShape(repeated, PanelLayouts.grid(2)))
    }

    @Test
    fun equalizingGivesEveryItemTheSameSize() {
        // One item beside a column of two: the column is as wide as the item, and its two share its height.
        assertEquals(listOf(0.5f, 0.5f), (PanelLayouts.equalize(PanelLayouts.resize(PanelLayouts.grid(3), emptyList(), 0, 0.8f)) as PanelSplit).shares)
        // One item beside a column whose second row holds two: the column has two items across, so it is twice as wide.
        val wide = PanelSplit(PanelAxis.Row, listOf(PanelSlot(0), PanelSplit(PanelAxis.Column, listOf(PanelSlot(1), PanelSplit(PanelAxis.Row, listOf(PanelSlot(2), PanelSlot(3)))))), listOf(0.9f, 0.1f))
        val shares = (PanelLayouts.equalize(wide) as PanelSplit).shares
        assertEquals(1f / 3, shares[0], 1e-6f)
        assertEquals(2f / 3, shares[1], 1e-6f)
    }
}
