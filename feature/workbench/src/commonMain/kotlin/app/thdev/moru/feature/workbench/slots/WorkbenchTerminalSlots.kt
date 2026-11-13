package app.thdev.moru.feature.workbench.slots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/**
 * The Task terminal: [pane] draws the terminals of one Task as [TerminalPaneSpec] describes; [typeInto] types text
 * into a live terminal without pressing Enter and [submitLine] types an app-written line and presses Enter. Both
 * return false when no such live terminal exists. [branchOf] names the branch a terminal works on, read off the UI
 * thread; none means no branch is shown.
 */
@Immutable
data class WorkbenchTerminalSlots(
    val pane: @Composable (spec: TerminalPaneSpec, modifier: Modifier) -> Unit,
    val typeInto: (sessionKey: String, text: String) -> Boolean,
    val submitLine: (sessionKey: String, line: String) -> Boolean,
    val branchOf: SessionBranchReader? = null,
)
