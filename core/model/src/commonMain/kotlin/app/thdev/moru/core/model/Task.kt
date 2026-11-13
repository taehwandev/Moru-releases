package app.thdev.moru.core.model

/**
 * One desired outcome inside a Project.
 *
 * A Task owns its title, visible status, timestamps, the current revision, and a
 * pointer to the current Result. It does not own raw processes, terminal widgets,
 * or Workflow definitions. Precise runtime state stays on Work Context children
 * and Runs; [status] is the single authoritative visible state.
 */
data class Task(
    val id: String,
    val projectId: String,
    val title: String,
    val status: TaskStatus = TaskStatus.Understanding,
    val currentRevisionId: String,
    val currentResultId: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
