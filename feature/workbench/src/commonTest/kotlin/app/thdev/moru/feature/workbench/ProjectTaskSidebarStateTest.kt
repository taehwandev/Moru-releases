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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectTaskSidebarStateTest {
    private fun project(
        id: String,
        name: String,
        availability: ProjectAvailability = ProjectAvailability.Available,
    ) = Project(
        id = id,
        displayName = name,
        roots = listOf(
            ProjectRoot(
                id = "$id-root",
                role = ProjectRootRole.Primary,
                lastKnownPath = "/work/$name",
                canonicalPath = "/work/$name",
                availability = availability,
            ),
        ),
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )

    private fun task(
        id: String,
        projectId: String,
        title: String,
        status: TaskStatus = TaskStatus.Working,
        attention: TaskAttention = TaskAttention.None,
        workContextLifecycle: app.thdev.moru.core.model.WorkContextPersistedLifecycle? = null,
    ) = TaskSummary(
        id = id,
        projectId = projectId,
        title = title,
        status = status,
        attention = attention,
        updatedAtEpochMillis = 0,
        workContextLifecycle = workContextLifecycle,
    )

    @Test
    fun taskRowReflectsWorkContextPresence() {
        val snapshot = ProjectCatalogSnapshot(
            projects = listOf(
                ProjectEntry(
                    project = project("p1", "Alpha"),
                    tasks = listOf(
                        task("t1", "p1", "With context", workContextLifecycle = app.thdev.moru.core.model.WorkContextPersistedLifecycle.Hibernated),
                        task("t2", "p1", "Without context"),
                    ),
                ),
            ),
            selectedProjectId = "p1",
        )

        val rows = projectTaskSidebarState(snapshot, emptySet()).projects.single().tasks

        assertTrue(rows.single { row -> row.id == "t1" }.hasWorkContext)
        assertFalse(rows.single { row -> row.id == "t2" }.hasWorkContext)
    }

    @Test
    fun aCollapsedSelectedProjectStaysCollapsed() {
        val snapshot = ProjectCatalogSnapshot(
            projects = listOf(
                ProjectEntry(
                    project = project("p1", "Alpha"),
                    tasks = listOf(task("t1", "p1", "Fix login")),
                ),
                ProjectEntry(
                    project = project("p2", "Beta"),
                    tasks = listOf(task("t2", "p2", "Update docs")),
                ),
            ),
            selectedProjectId = "p1",
            selectedTaskId = "t1",
        )

        val state = projectTaskSidebarState(snapshot, collapsedProjectIds = setOf("p1"))

        val alpha = state.projects.single { row -> row.id == "p1" }
        val beta = state.projects.single { row -> row.id == "p2" }
        assertFalse(alpha.expanded)
        assertTrue(alpha.selected)
        assertTrue(beta.expanded)
        assertTrue(state.projects.single { it.id == "p1" }.tasks.single().selected)
    }

    @Test
    fun aProjectNobodyCollapsedIsExpandedWithoutSelection() {
        val snapshot = ProjectCatalogSnapshot(
            projects = listOf(ProjectEntry(project("p2", "Beta"), tasks = emptyList())),
            selectedProjectId = null,
            selectedTaskId = null,
        )

        val state = projectTaskSidebarState(snapshot, collapsedProjectIds = emptySet())

        assertTrue(state.projects.single().expanded)
        assertFalse(state.projects.single().selected)
    }

    @Test
    fun attentionAndStatusRenderAsText() {
        val snapshot = ProjectCatalogSnapshot(
            projects = listOf(
                ProjectEntry(
                    project = project("p1", "Alpha"),
                    tasks = listOf(
                        task("t1", "p1", "Deploy", status = TaskStatus.NeedsApproval, attention = TaskAttention.NeedsApproval),
                    ),
                ),
            ),
            selectedProjectId = "p1",
        )

        val taskRow = projectTaskSidebarState(snapshot, emptySet()).projects.single().tasks.single()

        assertEquals(TaskDisplayState.NeedsApproval, taskRow.displayState)
        assertEquals(TaskGroup.NeedsAttention, taskRow.group)
    }

    @Test
    fun missingRootSurfacesAvailabilityLabel() {
        val snapshot = ProjectCatalogSnapshot(
            projects = listOf(
                ProjectEntry(
                    project = project("p1", "Alpha", availability = ProjectAvailability.Missing),
                    tasks = emptyList(),
                ),
            ),
        )

        val row = projectTaskSidebarState(snapshot, emptySet()).projects.single()

        assertFalse(row.isAvailable)
        assertEquals("Folder missing", row.availabilityLabel)
    }

    @Test
    fun emptyCatalogIsEmpty() {
        val state = projectTaskSidebarState(ProjectCatalogSnapshot(projects = emptyList()), emptySet())
        assertTrue(state.isEmpty)
    }
}
