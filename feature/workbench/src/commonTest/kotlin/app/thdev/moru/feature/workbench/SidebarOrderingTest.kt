package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.ProjectCatalogSnapshot
import app.thdev.moru.core.domain.ProjectEntry
import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.core.model.Project
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.TaskSortOrder
import app.thdev.moru.core.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarOrderingTest {
    private fun task(id: String, created: Long, updated: Long, project: String = "p") = TaskSummary(
        id, project, id, TaskStatus.Working, TaskAttention.None, updated,
        lastAgent = id, createdAtEpochMillis = created,
    )
    private fun project(id: String, tasks: List<TaskSummary>) = ProjectEntry(
        Project(id, id, emptyList(), createdAtEpochMillis = 0, updatedAtEpochMillis = 0), tasks,
    )

    @Test
    fun defaultAppendsNewTasksAndActivityDoesNotMoveThemOrChangeTheLastAgent() {
        val tasks = listOf(task("old", 10, 500), task("new", 20, 100))
        val snapshot = ProjectCatalogSnapshot(listOf(project("p", tasks)), "p", "old")
        val state = projectTaskSidebarState(snapshot, emptySet())
        assertEquals(listOf("old", "new"), state.projects.single().tasks.map { it.id })
        assertEquals("old", state.lastUsedAgent, "Agent selection still follows activity, not display order")
        assertEquals("old", state.selectedTaskId)
        val changed = snapshot.copy(projects = listOf(project("p", tasks.map {
            it.copy(title = "Renamed", updatedAtEpochMillis = 999, status = TaskStatus.Done)
        })))
        assertEquals(listOf("old", "new"), projectTaskSidebarState(changed, emptySet()).projects.single().tasks.map { it.id })
    }

    @Test
    fun newestFirstUsesCreationTimeAndEqualTimesHaveAStableIdOrder() {
        val tasks = listOf(task("z", 10, 999), task("a", 10, 1000), task("new", 20, 0))
        val snapshot = ProjectCatalogSnapshot(listOf(project("p", tasks)))
        fun ids(order: TaskSortOrder) = projectTaskSidebarState(snapshot, emptySet(), GeneralSettings(taskSortOrder = order))
            .projects.single().tasks.map { it.id }
        assertEquals(listOf("a", "z", "new"), ids(TaskSortOrder.OldestFirst))
        assertEquals(listOf("new", "a", "z"), ids(TaskSortOrder.NewestFirst))
    }

    @Test
    fun manualOrderIgnoresRemovedAndDuplicateIdsAndAppendsNewTasksPerProject() {
        val snapshot = ProjectCatalogSnapshot(listOf(
            project("p", listOf(task("first", 1, 9), task("second", 2, 8), task("new", 3, 7))),
            project("other", listOf(task("other-task", 1, 2, "other"))),
        ))
        val general = GeneralSettings(
            taskSortOrder = TaskSortOrder.Manual,
            projectOrder = listOf("removed", "other", "other", "p"),
            taskOrder = mapOf("p" to listOf("second", "removed", "second", "other-task", "first")),
        )
        val state = projectTaskSidebarState(snapshot, emptySet(), general)
        assertEquals(listOf("other", "p"), state.projects.map { it.id })
        assertEquals(listOf("second", "first", "new"), state.projects.last().tasks.map { it.id })
        assertEquals(listOf("other-task"), state.projects.first().tasks.map { it.id })
        val timed = projectTaskSidebarState(snapshot, emptySet(), general.copy(taskSortOrder = TaskSortOrder.OldestFirst))
        assertEquals(listOf("other", "p"), timed.projects.map { it.id }, "Project order is independent")
        assertEquals(listOf("first", "second", "new"), timed.projects.last().tasks.map { it.id })
    }
}
