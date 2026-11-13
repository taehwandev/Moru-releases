package app.thdev.moru.feature.workbench.slots

import app.thdev.moru.core.terminal.TerminalSessionManager

/** One Task's Code panel as the code slot shows it: the Task, the folder it works in and the terminals it reads. */
data class CodePanelSpec(
    val taskKey: String,
    val projectFolder: String,
    val sessionManager: TerminalSessionManager,
)
