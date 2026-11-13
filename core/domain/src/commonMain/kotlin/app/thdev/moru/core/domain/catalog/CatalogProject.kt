package app.thdev.moru.core.domain.catalog

import app.thdev.moru.core.model.ProjectAvailability

/** Durable navigation metadata; no root handles or live runtime state. */
data class CatalogProject(
    val id: String,
    val rootId: String,
    val name: String,
    val path: String,
    val availability: ProjectAvailability,
    val lastTaskId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val closed: Boolean,
)
