package app.thdev.moru.core.domain

import app.thdev.moru.core.model.Project

/** Outcome of [ProjectCatalogRepository.registerProject]. */
data class ProjectRegistration(
    val project: Project,
    val alreadyRegistered: Boolean,
)
