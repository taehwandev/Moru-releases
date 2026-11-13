package app.thdev.moru.feature.workbench.slots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/** The Task's Code panel: [pane] draws the code the Task's chosen terminal is changing as [CodePanelSpec] describes. */
@Immutable
data class WorkbenchCodeSlots(
    val pane: @Composable (spec: CodePanelSpec, modifier: Modifier) -> Unit,
)
