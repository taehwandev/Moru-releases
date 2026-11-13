package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.TaskAttention

/**
 * One Task row in the sidebar. State is carried by [displayState] and its labels,
 * never by color alone. [projectId] and [projectName] let a group span Projects.
 */
data class TaskRow(
    val id: String,
    val projectId: String,
    val projectName: String,
    val title: String,
    val displayState: TaskDisplayState,
    val group: TaskGroup,
    val attention: TaskAttention,
    val hasWorkContext: Boolean,
    val selected: Boolean,
    /** The newest meaningful line the Task's terminal printed, or null; it shows what the agent is doing now. */
    val lastLine: String? = null,
    /** The agent the Task was last started with, by name, or null. */
    val lastAgent: String? = null,
)
