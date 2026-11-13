package app.thdev.moru.core.domain.catalog

import app.thdev.moru.core.model.TaskStatus

/** One current Task revision; terminal exit does not change [status]. */
data class CatalogTask(
    val id: String,
    val projectId: String,
    val revisionId: String,
    val title: String,
    val desiredOutcome: String,
    val status: TaskStatus,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
