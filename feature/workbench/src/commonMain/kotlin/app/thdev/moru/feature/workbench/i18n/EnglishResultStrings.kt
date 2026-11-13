package app.thdev.moru.feature.workbench.i18n

import app.thdev.moru.feature.workbench.result.ResultTab

internal object EnglishResultStrings : ResultStrings {
    override fun tab(tab: ResultTab) = when (tab) {
        ResultTab.Code -> "Code"
    }
    override val needsTask = "Open or start a Task to use this tool."
    override val openResult = "Open the result panel"
    override val resizeResult = "Resize Result pane"
    override fun resultOf(taskTitle: String) = "Of $taskTitle"
    override val detachResult = "Open Result in separate window"
    override val dockResult = "Move Result back to the main window"
    override fun closeTool(tool: ResultTab) = "Close ${tab(tool)}"
    override val detachedResult = "Result is in a separate window"
    override fun detachedTitle(task: String) = "$task · Result"
    override val resetPaneSize = "Reset pane size"
    override val noChanges = "No changes yet. What the agent edits shows up here."
    override val codeNotRepository = "This folder is not in a Git repository."
    override val reviseLabel = "Want something different?"
    override val revisePlaceholder = "Say what should change"
    override val reviseButton = "Ask the agent"
    override val revised = "Typed into the agent's terminal. Press Enter there to send it."
    override val reviseNoAgent = "No running agent terminal for this Task, so nothing was typed."
}
