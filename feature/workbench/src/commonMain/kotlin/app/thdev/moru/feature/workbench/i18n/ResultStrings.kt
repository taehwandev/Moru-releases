package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.feature.workbench.result.ResultTab

/** Text of the Result pane: its tab names and what an empty tab says. */
internal interface ResultStrings {
    fun tab(tab: ResultTab): String
    val openResult: String
    val resizeResult: String
    fun resultOf(taskTitle: String): String
    val detachResult: String
    val dockResult: String
    fun closeTool(tool: ResultTab): String
    val detachedResult: String
    fun detachedTitle(task: String): String
    val resetPaneSize: String
    /** What a Task tool says while no Task is open. */
    val needsTask: String
    val noChanges: String
    val codeNotRepository: String
    val reviseLabel: String
    val revisePlaceholder: String
    val reviseButton: String
    val revised: String
    val reviseNoAgent: String
}
