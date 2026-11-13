package app.thdev.moru.app

import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectCatalogSnapshot

/**
 * The catalog as a Project window sees it: only [projectId], with the window's own selected Task kept here. The one
 * saved selection belongs to the main window; choosing a Task here still records it as the Project's last Task, so the
 * Project reopens on it wherever it is shown next.
 */
internal class ProjectWindowCatalog(
    private val base: ProjectCatalogRepository,
    private val projectId: String,
) : ProjectCatalogRepository by base {
    private var selectedTaskId: String? = null

    override fun loadCatalog(): ProjectCatalogSnapshot {
        val all = base.loadCatalog()
        val entry = all.projects.firstOrNull { it.project.id == projectId }
        val taskIds = entry?.tasks?.map { it.id }.orEmpty()
        val task = selectedTaskId?.takeIf { it in taskIds }
            ?: entry?.project?.lastSelectedTaskId?.takeIf { it in taskIds }
            ?: taskIds.firstOrNull()
        return ProjectCatalogSnapshot(
            projects = listOfNotNull(entry),
            selectedProjectId = entry?.project?.id,
            selectedTaskId = task,
        )
    }

    override fun updateSelection(selectedProjectId: String?, selectedTaskId: String?) {
        if (selectedProjectId != projectId) return
        this.selectedTaskId = selectedTaskId
        if (selectedTaskId == null) return
        // The repository records a Project's last Task only with the saved selection, which is the main window's:
        // record it there and put the main window's selection back at once, unless the main window is on this Project
        // too, where putting it back would record its older Task over this one.
        val main = base.loadCatalog()
        base.updateSelection(projectId, selectedTaskId)
        if (main.selectedProjectId != projectId) base.updateSelection(main.selectedProjectId, main.selectedTaskId)
    }
}
