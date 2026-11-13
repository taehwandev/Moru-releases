package app.thdev.moru.feature.workbench.slots

import androidx.compose.runtime.Immutable

/**
 * What the workbench shows and does through other features: the terminal pane and its input on one side, the sessions
 * the sidebar lists, and the Code panel when the app can read code ([code] is null when it cannot, and the Task then has no Code tab). The app supplies
 * the real ones; tests supply stand-ins.
 */
@Immutable
data class WorkbenchPaneSlots(
    val terminal: WorkbenchTerminalSlots,
    val sessions: WorkbenchSessionSlots,
    val code: WorkbenchCodeSlots? = null,
)
