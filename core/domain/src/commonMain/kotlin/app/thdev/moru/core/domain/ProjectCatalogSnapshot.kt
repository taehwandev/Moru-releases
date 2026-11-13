package app.thdev.moru.core.domain

import app.thdev.moru.core.model.Project

/**
 * Bounded, restart-restorable catalog navigation state. [closedProjects] are the most recently closed Projects, so they
 * can be offered for reopening; they are not part of [projects] and are never selected.
 */
data class ProjectCatalogSnapshot(
    val projects: List<ProjectEntry>,
    val selectedProjectId: String? = null,
    val selectedTaskId: String? = null,
    val closedProjects: List<Project> = emptyList(),
)
