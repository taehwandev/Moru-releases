package app.thdev.moru.feature.workbench.beside

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropPreview
import app.thdev.moru.core.designsystem.component.MoruPanelDragState
import app.thdev.moru.core.designsystem.component.MoruShareSplit
import app.thdev.moru.core.model.panel.PanelLayouts
import app.thdev.moru.core.model.panel.PanelNode
import app.thdev.moru.core.model.panel.PanelSplit
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.i18n.resultStrings
import app.thdev.moru.feature.workbench.i18n.workbenchStrings

/**
 * The selected Task's terminal area ([main]) with [tasks] side by side after it, each in its share of the width as
 * [split] keeps it. With no Task beside, [main] takes the whole area. While a Task is beside, [main] sits under the
 * same kind of header, naming the selected Task ([mainTitle]); dragging a Task beside by its header onto it trades the
 * two through [onDropOnMain]. [beside] gets the header's modifier and whether a drag would land on it.
 */
@Composable
internal fun BesideTasksLayout(
    tasks: List<BesideTask>,
    split: PanelNode?,
    onSplitChange: (PanelNode) -> Unit,
    modifier: Modifier,
    main: @Composable (Modifier) -> Unit,
    beside: @Composable (task: BesideTask, modifier: Modifier, headerModifier: Modifier, dropHere: Boolean) -> Unit,
    mainTitle: String = "",
    mainState: TaskDisplayState? = null,
    onDropOnMain: (taskId: String) -> Unit = {},
) {
    if (tasks.isEmpty()) {
        main(modifier)
        return
    }
    val strings = workbenchStrings()
    val drag = remember { MoruPanelDragState(allowEdges = false) }
    val root = PanelLayouts.keepIfSameShape(split, PanelLayouts.row(tasks.size + 1))
    MoruDropPreview(drag)
    MoruShareSplit(
        vertical = false,
        shares = (root as PanelSplit).shares,
        onSeamMove = { seam, share -> onSplitChange(PanelLayouts.resize(root, emptyList(), seam, share)) },
        onSeamReset = { seam -> onSplitChange(PanelLayouts.even(root, emptyList(), seam)) },
        dividerLabel = strings.resizeBeside,
        resetLabel = resultStrings().resetPaneSize,
        minSize = 240.dp,
        modifier = modifier,
    ) { index, childModifier ->
        if (index == 0) Column(childModifier.then(drag.dropTarget(MainKey))) {
            BesideMainHeader(mainTitle, mainState, dropHere = drag.target == MainKey)
            main(Modifier.weight(1f).fillMaxWidth())
        } else {
            val task = tasks[index - 1]
            beside(
                task,
                childModifier.then(drag.dropTarget(task.id)),
                drag.handle(task.id) { from, to, _ -> if (to == MainKey) onDropOnMain(from as String) },
                drag.target == task.id,
            )
        }
    }
}

/** The drop key of the selected Task's pane; Task ids never take this form. */
private const val MainKey = "beside:main"
