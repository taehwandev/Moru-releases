package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.workbench.ProjectRow
import app.thdev.moru.feature.workbench.ProjectTaskSidebarState

/** Applies [withSessionActivity] and the newest printed line to every Task that has a live session; the rest stay as persisted. */
fun ProjectTaskSidebarState.withSessionActivities(
    activities: Map<String, SessionActivity>,
    lastLines: Map<String, String> = emptyMap(),
): ProjectTaskSidebarState =
    if (activities.isEmpty() && lastLines.isEmpty()) {
        this
    } else {
        copy(projects = projects.map { project -> project.withSessionActivities(activities, lastLines) })
    }

private fun ProjectRow.withSessionActivities(activities: Map<String, SessionActivity>, lastLines: Map<String, String>): ProjectRow =
    copy(
        tasks = tasks.map { task ->
            val overlaid = activities[task.id]?.let(task::withSessionActivity) ?: task
            lastLines[task.id]?.let { line -> overlaid.copy(lastLine = line) } ?: overlaid
        },
    )
