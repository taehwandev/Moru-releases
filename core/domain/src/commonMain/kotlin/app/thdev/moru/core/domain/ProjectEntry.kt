package app.thdev.moru.core.domain

import app.thdev.moru.core.model.Project

/** One Project plus its bounded Task summaries. */
data class ProjectEntry(
    val project: Project,
    val tasks: List<TaskSummary>,
)
