package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.model.SavedProjectWindow

/**
 * The Projects open in windows of their own, kept in the saved layout so they reopen where they were. One Project is in
 * one window: opening it again asks that window to come forward instead ([forwardRequest]).
 */
class ProjectWindows(
    private val layout: WorkbenchLayoutKeeper,
    private val catalog: ProjectCatalogRepository? = null,
) {
    val windows: List<SavedProjectWindow> get() = layout.layout.projectWindows

    /** The Project whose window should come to the front, set by [open] or [bringForward] and cleared once it has. */
    var forwardRequest by mutableStateOf<String?>(null)
        private set

    /** Each open Project's name for its window's title, as [refresh] last read it. */
    var names by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /**
     * Reads the catalog once for every window: their titles, and the windows whose Project was closed or forgotten,
     * which close so they do not come back empty on the next launch.
     */
    fun refresh() {
        val projects = catalog?.loadCatalog()?.projects?.map { it.project } ?: return
        names = projects.associate { it.id to it.displayName }
        windows.filter { window -> projects.none { it.id == window.projectId } }.forEach { close(it.projectId) }
    }

    fun isOpen(projectId: String): Boolean = windows.any { it.projectId == projectId }

    fun open(projectId: String) {
        if (!isOpen(projectId)) layout.update { it.copy(projectWindows = it.projectWindows + SavedProjectWindow(projectId)) }
        forwardRequest = projectId
    }

    fun bringForward(projectId: String) {
        if (isOpen(projectId)) forwardRequest = projectId
    }

    fun forwarded(projectId: String) {
        if (forwardRequest == projectId) forwardRequest = null
    }

    /** Closes the Project's window; the Project stays and shows in the main window when chosen there. */
    fun close(projectId: String) {
        layout.update { saved -> saved.copy(projectWindows = saved.projectWindows.filterNot { it.projectId == projectId }) }
        forwarded(projectId)
    }

    /** Keeps where the Project's window is now, in dp. */
    fun moved(projectId: String, x: Float?, y: Float?, width: Float, height: Float) {
        layout.update { saved ->
            saved.copy(projectWindows = saved.projectWindows.map { window ->
                if (window.projectId == projectId) window.copy(x = x, y = y, width = width, height = height) else window
            })
        }
    }
}
