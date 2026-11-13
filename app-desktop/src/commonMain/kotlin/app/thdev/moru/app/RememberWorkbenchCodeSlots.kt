package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.thdev.moru.core.model.SavedCodePanel
import app.thdev.moru.feature.codepanel.CodePanelSource
import app.thdev.moru.feature.codepanel.TaskCodePanel
import app.thdev.moru.feature.codepanel.rememberCodePanelState
import app.thdev.moru.feature.workbench.slots.WorkbenchCodeSlots

/**
 * The workbench's Code panel slot, reading code through [source]; null without one, so the Task has no Code tab. Each
 * Task's checkouts start from [saved] and every change is reported to [onSavedChange], whether or not there is a source.
 */
@Composable
internal fun rememberWorkbenchCodeSlots(
    source: CodePanelSource?,
    saved: Map<String, SavedCodePanel>,
    onSavedChange: (Map<String, SavedCodePanel>) -> Unit,
): WorkbenchCodeSlots? {
    val state = rememberCodePanelState(saved, onSavedChange)
    return remember(source, state) {
        source?.let { reader ->
            WorkbenchCodeSlots { spec, modifier ->
                TaskCodePanel(reader, state, spec.taskKey, spec.projectFolder, spec.sessionManager, modifier)
            }
        }
    }
}
