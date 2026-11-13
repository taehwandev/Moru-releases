package app.thdev.moru.core.domain

import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.core.model.WorkContextPersistedLifecycle

/**
 * Metadata-only Task row for the sidebar. [workContextLifecycle] is the persisted
 * Work Context lifecycle when one exists (a terminal/context has been opened for
 * the Task), or null; it is a bounded descriptor, never a live-process claim.
 */
data class TaskSummary(
    val id: String,
    val projectId: String,
    val title: String,
    val status: TaskStatus,
    val attention: TaskAttention,
    val updatedAtEpochMillis: Long,
    val workContextLifecycle: WorkContextPersistedLifecycle? = null,
    /** The agent the Task was last started with, by name, or null. */
    val lastAgent: String? = null,
    /** When the Task was created, which orders Tasks that are numbered together. */
    val createdAtEpochMillis: Long = 0L,
)
