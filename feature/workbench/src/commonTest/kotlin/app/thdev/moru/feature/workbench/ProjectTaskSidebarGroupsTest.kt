package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.ProjectCatalogSnapshot
import app.thdev.moru.core.domain.ProjectEntry
import app.thdev.moru.core.domain.TaskSummary
import app.thdev.moru.core.model.Project
import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.ProjectRoot
import app.thdev.moru.core.model.ProjectRootRole
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectTaskSidebarGroupsTest {
    private fun project(id: String, name: String) = Project(
        id = id,
        displayName = name,
        roots = listOf(
            ProjectRoot(
                id = "$id-root",
                role = ProjectRootRole.Primary,
                lastKnownPath = "/work/$name",
                canonicalPath = "/work/$name",
                availability = ProjectAvailability.Available,
            ),
        ),
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )

    private fun task(
        id: String,
        projectId: String,
        status: TaskStatus,
        attention: TaskAttention = TaskAttention.None,
    ) = TaskSummary(
        id = id,
        projectId = projectId,
        title = "Task $id",
        status = status,
        attention = attention,
        updatedAtEpochMillis = 0,
        workContextLifecycle = null,
    )

    private fun snapshot(vararg entries: ProjectEntry) =
        ProjectCatalogSnapshot(projects = entries.toList(), selectedProjectId = entries.firstOrNull()?.project?.id)

    @Test
    fun pendingApprovalOutranksQuietWorkingStatus() {
        val state = projectTaskSidebarState(
            snapshot(
                ProjectEntry(
                    project("p1", "Alpha"),
                    listOf(task("t1", "p1", TaskStatus.Working, TaskAttention.NeedsApproval)),
                ),
            ),
            emptySet(),
        )

        val row = state.projects.single().tasks.single()

        assertEquals(TaskDisplayState.NeedsApproval, row.displayState)
        assertEquals(TaskGroup.NeedsAttention, row.group)
    }

    @Test
    fun stoppedTaskIsReviewNotDone() {
        val state = projectTaskSidebarState(
            snapshot(ProjectEntry(project("p1", "Alpha"), listOf(task("t1", "p1", TaskStatus.Stopped)))),
            emptySet(),
        )

        val row = state.projects.single().tasks.single()

        assertEquals(TaskGroup.Review, row.group)
        assertEquals(TaskDisplayState.Stopped, row.displayState)
    }

}
