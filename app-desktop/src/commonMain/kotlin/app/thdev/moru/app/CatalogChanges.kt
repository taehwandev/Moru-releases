package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectRegistration
import app.thdev.moru.core.domain.ProjectRelocation
import app.thdev.moru.core.model.Task
import app.thdev.moru.core.model.TaskStatus

/**
 * Tells every window that the catalog changed, so a Task made, renamed or closed in one window shows in the others.
 * The catalog has no change events of its own; each window reads it again when [revision] moves.
 */
internal object CatalogChanges {
    var revision by mutableIntStateOf(0)
        private set

    fun changed() { revision += 1 }
}

/**
 * [base], announcing each change it makes through [CatalogChanges]. Choosing a Project or Task is not a change. The
 * window that made a change has read the catalog again already, so [othersChanged] tells it whether anyone else did.
 */
internal class AnnouncingCatalog(private val base: ProjectCatalogRepository) : ProjectCatalogRepository by base {
    private var ownChanges = 0
    private var seenRevision = 0
    private var seenOwnChanges = 0

    private inline fun <T> announcing(change: () -> T): T = change().also {
        ownChanges += 1
        CatalogChanges.changed()
    }

    /** Whether a change since the last call came from another window, at [revision] of [CatalogChanges]. */
    fun othersChanged(revision: Int): Boolean {
        val others = (revision - seenRevision) > (ownChanges - seenOwnChanges)
        seenRevision = revision
        seenOwnChanges = ownChanges
        return others
    }

    override fun registerProject(displayName: String, rootPath: String): ProjectRegistration =
        announcing { base.registerProject(displayName, rootPath) }
    override fun closeProject(projectId: String) = announcing { base.closeProject(projectId) }
    override fun forgetClosedProject(projectId: String) = announcing { base.forgetClosedProject(projectId) }
    override fun renameProject(projectId: String, displayName: String) = announcing { base.renameProject(projectId, displayName) }
    override fun relocateProject(projectId: String, rootPath: String): ProjectRelocation =
        announcing { base.relocateProject(projectId, rootPath) }
    override fun createTask(projectId: String, title: String, desiredOutcome: String): Task =
        announcing { base.createTask(projectId, title, desiredOutcome) }
    override fun updateTaskStatus(taskId: String, status: TaskStatus) = announcing { base.updateTaskStatus(taskId, status) }
    override fun setTaskAgent(taskId: String, agent: String?) = announcing { base.setTaskAgent(taskId, agent) }
    override fun renameTask(taskId: String, title: String) = announcing { base.renameTask(taskId, title) }
    override fun retitleTasks(titles: Map<String, String>) = announcing { base.retitleTasks(titles) }
    override fun deleteTask(taskId: String) = announcing { base.deleteTask(taskId) }
}
