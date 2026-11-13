package app.thdev.moru.feature.workbench.result

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.feature.workbench.slots.WorkbenchPaneSlots

/** Binds the active Task's Result controls without changing its tab identity. */
@Composable
internal fun TaskResultPane(
    selected: ResultTab,
    taskKey: String,
    workspacePath: String,
    pane: ResultPaneState,
    sessions: TerminalSessionManager,
    slots: WorkbenchPaneSlots,
    modifier: Modifier = Modifier,
    taskPicker: (@Composable () -> Unit)? = null,
) {
    ResultPane(
        selected = selected,
        modifier = modifier,
        taskPicker = taskPicker,
        placementAction = if (pane.detached) {
            ResultPanePlacementAction.Dock { pane.restore(taskKey) }
        } else ResultPanePlacementAction.Detach { pane.detach() },
    ) {
        ResultPaneContent(selected, taskKey, workspacePath, sessions, slots)
    }
}
