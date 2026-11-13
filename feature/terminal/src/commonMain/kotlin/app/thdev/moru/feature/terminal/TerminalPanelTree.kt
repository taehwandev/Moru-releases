package app.thdev.moru.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruShareSplit
import app.thdev.moru.core.model.panel.PanelAxis
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.model.panel.PanelSlot
import app.thdev.moru.core.model.panel.PanelSplit

/**
 * [root] with [cell] drawn for each slot. Each slot's composition is kept by its key in [keys] so moving, swapping, or
 * closing a neighbour never disposes or reloads the remaining panes.
 */
@Composable
internal fun TerminalPanelTree(
    root: PanelNode,
    keys: List<String>,
    onChange: (PanelNode) -> Unit,
    modifier: Modifier,
    cell: @Composable (index: Int, Modifier) -> Unit,
) {
    val currentCell by rememberUpdatedState(cell)
    val movableCells = remember { mutableMapOf<String, @Composable (Int, Modifier) -> Unit>() }
    movableCells.keys.retainAll(keys.toSet())
    TerminalPanelBranch(root, root, emptyList(), onChange, modifier) { index, cellModifier ->
        val key = keys.getOrNull(index)
        if (key != null) {
            movableCells.getOrPut(key) {
                movableContentOf { slotIndex, slotModifier -> currentCell(slotIndex, slotModifier) }
            }.invoke(index, cellModifier)
        } else {
            currentCell(index, cellModifier)
        }
    }
}

@Composable
private fun TerminalPanelBranch(
    root: PanelNode,
    node: PanelNode,
    path: List<Int>,
    onChange: (PanelNode) -> Unit,
    modifier: Modifier,
    cell: @Composable (index: Int, Modifier) -> Unit,
) {
    when (node) {
        is PanelSlot -> cell(node.index, modifier)
        is PanelSplit -> MoruShareSplit(
            vertical = node.axis == PanelAxis.Column,
            shares = node.shares,
            onSeamMove = { seam, share -> onChange(PanelLayouts.resize(root, path, seam, share)) },
            onSeamReset = { seam -> onChange(PanelLayouts.even(root, path, seam)) },
            dividerLabel = "Resize terminals",
            resetLabel = "Reset terminal sizes",
            minSize = 160.dp,
            modifier = modifier,
        ) { i, childModifier ->
            TerminalPanelBranch(root, node.children[i], path + i, onChange, childModifier, cell)
        }
    }
}
