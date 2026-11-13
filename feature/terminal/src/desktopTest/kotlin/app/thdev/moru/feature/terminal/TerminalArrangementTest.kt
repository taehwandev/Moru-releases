package app.thdev.moru.feature.terminal

import app.thdev.moru.core.model.panel.PanelAxis
import app.thdev.moru.core.model.panel.PanelEdge
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelSlot
import app.thdev.moru.core.model.panel.PanelSplit
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalArrangementTest {
    @Test
    fun aSplitPutsTheNewTerminalOnItsSideAndEvensEveryTerminal() {
        val requests = mutableMapOf<String, Pair<String, PanelEdge>>()
        val arrangement = TerminalArrangement { id -> requests.remove(id) }
        arrangement.resolve(null, listOf("a", "b"))
        // The user had made "a" much wider.
        val dragged = PanelLayouts.resize(PanelLayouts.grid(2), emptyList(), 0, 0.8f)

        // Command-Shift-D on "a": "c" goes under it, and both columns and both rows are made even.
        requests["c"] = "a" to PanelEdge.Bottom
        val split = arrangement.resolve(dragged, listOf("a", "b", "c")) as PanelSplit

        assertEquals(PanelAxis.Row, split.axis)
        assertEquals(listOf(0.5f, 0.5f), split.shares)
        val column = split.children[0] as PanelSplit
        assertEquals(PanelAxis.Column, column.axis)
        assertEquals(listOf(PanelSlot(0), PanelSlot(2)), column.children)
        assertEquals(listOf(0.5f, 0.5f), column.shares)
    }

    @Test
    fun aNewTerminalNotOpenedByASplitGoesIntoTheLargestPlaceAndLeavesTheOthersAlone() {
        val arrangement = TerminalArrangement { null }
        arrangement.resolve(null, listOf("a", "b"))
        val dragged = PanelLayouts.resize(PanelLayouts.grid(2), emptyList(), 0, 0.8f)

        val added = arrangement.resolve(dragged, listOf("a", "b", "c")) as PanelSplit

        // "a" was the largest place, so "c" takes half of it and "b" keeps its fifth.
        assertEquals(listOf(0, 2, 1), PanelLayouts.slots(added))
        assertEquals(0.4f, added.shares[0], 1e-6f)
        assertEquals(0.4f, added.shares[1], 1e-6f)
        assertEquals(0.2f, added.shares[2], 1e-6f)
    }
}
