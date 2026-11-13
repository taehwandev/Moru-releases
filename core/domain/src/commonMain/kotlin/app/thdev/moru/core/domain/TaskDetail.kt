package app.thdev.moru.core.domain

import app.thdev.moru.core.model.TaskStatus

/**
 * The current revision detail for one Task: its request text and visible status.
 * This is the read model behind the Task conversation view; it carries the
 * desired outcome the user typed, not runtime child state.
 */
data class TaskDetail(
    val id: String,
    val projectId: String,
    val title: String,
    val status: TaskStatus,
    val desiredOutcome: String,
    val revisionOrdinal: Int,
    val updatedAtEpochMillis: Long,
)
