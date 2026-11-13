package app.thdev.moru.feature.workbench

/** What the sidebar can do to a Task: the user's own decisions about it. Each takes the Task's id. */
data class TaskRowActions(
    val onRename: (taskId: String, title: String) -> Unit = { _, _ -> },
    val onDelete: (taskId: String) -> Unit = {},
    /** Tasks open beside the selected one; their menu offers to close them there. */
    val besideTaskIds: Set<String> = emptySet(),
    /** The selected Task's Project: only its other Tasks can open beside. */
    val besideProjectId: String? = null,
    /** Opens the Task's terminals beside the selected Task, or closes them there; none hides the entry. */
    val onToggleBeside: ((taskId: String) -> Unit)? = null,
    /** Opens a Project in a window of its own (Experimental); none hides the entry. Takes the Project's id. */
    val onOpenProjectWindow: ((projectId: String) -> Unit)? = null,
)
