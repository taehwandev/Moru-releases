package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.ProjectCatalogSnapshot
import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.core.model.TaskAttention

/**
 * Build the sidebar state from a bounded catalog snapshot. Every Project is
 * expanded except those in [collapsedProjectIds], the ones the user folded away
 * with their disclosure. State is rendered as icon and text, never color alone.
 */
fun projectTaskSidebarState(
    snapshot: ProjectCatalogSnapshot,
    collapsedProjectIds: Set<String>,
    general: GeneralSettings = GeneralSettings(),
): ProjectTaskSidebarState {
    val projects = SidebarOrdering.projects(snapshot.projects, general.projectOrder).map { entry ->
        val project = entry.project
        val projectSelected = project.id == snapshot.selectedProjectId
        ProjectRow(
            id = project.id,
            displayName = project.displayName,
            rootPath = project.primaryRoot?.let { root -> root.canonicalPath ?: root.lastKnownPath },
            availabilityLabel = project.availability.sidebarLabel(),
            isAvailable = project.availability == ProjectAvailability.Available,
            expanded = project.id !in collapsedProjectIds,
            selected = projectSelected,
            tasks = SidebarOrdering.tasks(entry.tasks, project.id, general).map { task ->
                val displayState = deriveTaskDisplayState(task.status, task.attention)
                val presentation = displayState.presentation()
                TaskRow(
                    id = task.id,
                    projectId = project.id,
                    projectName = project.displayName,
                    title = task.title,
                    displayState = displayState,
                    group = presentation.group,
                    attention = task.attention,
                    hasWorkContext = task.workContextLifecycle != null,
                    selected = task.id == snapshot.selectedTaskId,
                    lastAgent = task.lastAgent,
                )
            },
        )
    }
    return ProjectTaskSidebarState(
        projects = projects,
        selectedProjectId = snapshot.selectedProjectId,
        selectedTaskId = snapshot.selectedTaskId,
        lastUsedAgent = snapshot.projects.flatMap { entry -> entry.tasks }
            .filter { task -> task.lastAgent != null }
            .maxByOrNull { task -> task.updatedAtEpochMillis }?.lastAgent,
        closedProjects = snapshot.closedProjects.mapNotNull { project ->
            project.primaryRoot?.let { root -> ClosedProjectRow(project.id, project.displayName, root.canonicalPath ?: root.lastKnownPath) }
        },
        autoCollapseInactiveSidebar = general.autoCollapseInactiveSidebar,
    )
}

private fun ProjectAvailability.sidebarLabel(): String? =
    when (this) {
        ProjectAvailability.Available -> null
        ProjectAvailability.Missing -> "Folder missing"
        ProjectAvailability.PermissionDenied -> "No access"
    }
