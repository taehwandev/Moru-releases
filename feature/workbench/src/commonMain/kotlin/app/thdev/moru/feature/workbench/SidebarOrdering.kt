package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.ProjectEntry
import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.core.model.TaskSortOrder

/** Display ordering leaves catalog activity, ownership and selection untouched. */
internal object SidebarOrdering {
    fun projects(entries: List<ProjectEntry>, savedIds: List<String>): List<ProjectEntry> =
        bySavedIds(entries, savedIds) { it.project.id }

    fun tasks(tasks: List<TaskSummary>, projectId: String, general: GeneralSettings): List<TaskSummary> {
        val oldestFirst = tasks.sortedWith(compareBy<TaskSummary> { it.createdAtEpochMillis }.thenBy { it.id })
        return when (general.taskSortOrder) {
            TaskSortOrder.OldestFirst -> oldestFirst
            TaskSortOrder.NewestFirst -> tasks.sortedWith(
                compareByDescending<TaskSummary> { it.createdAtEpochMillis }.thenBy { it.id },
            )
            TaskSortOrder.Manual -> bySavedIds(oldestFirst, general.taskOrder[projectId].orEmpty()) { it.id }
        }
    }

    private fun <T> bySavedIds(items: List<T>, savedIds: List<String>, id: (T) -> String): List<T> {
        val ranks = savedIds.distinct().withIndex().associate { it.value to it.index }
        return items.sortedBy { ranks[id(it)] ?: Int.MAX_VALUE }
    }
}
