package app.thdev.moru.core.domain

import app.thdev.moru.core.model.Task
import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.core.model.WorkContextDescriptor

/**
 * The single authority for the global Project/Task catalog that backs the
 * Workbench sidebar. It owns registration and navigation metadata only; it never
 * starts a PTY, watcher, agent process, or Run, and never becomes a second store
 * for Project files or immutable Run evidence.
 *
 * The contract is synchronous to match the existing repository style; the Room
 * implementation bridges coroutines internally.
 */
interface ProjectCatalogRepository {
    /**
     * Restore bounded Project rows, their Task summaries, and the persisted
     * selection. Loading summaries is metadata only and must not hydrate runtime
     * children.
     */
    fun loadCatalog(): ProjectCatalogSnapshot

    /**
     * Register a Project for [rootPath]. If an accessible root canonicalizes to an
     * already-registered root, the existing Project is returned with
     * [ProjectRegistration.alreadyRegistered] set instead of creating a duplicate.
     */
    fun registerProject(displayName: String, rootPath: String): ProjectRegistration

    /**
     * Close a Project: it is no longer listed, and if it was selected the selection is cleared. Its Tasks and
     * folder are untouched, and registering the same root again restores it. Unknown ids are ignored.
     */
    fun closeProject(projectId: String)

    /**
     * Delete a closed Project from the catalog together with its Tasks, their revisions and work contexts, and clear
     * the selection if it pointed into them. Only a closed Project can be deleted; an open one and an unknown id are
     * ignored. The folder and its files are never touched, and opening the folder again registers a new Project.
     */
    fun forgetClosedProject(projectId: String)

    /**
     * Change a Project's display name. The name is trimmed, line breaks become spaces and it is cut to a bounded
     * length; a blank name or an unknown id changes nothing. The folder is never touched.
     */
    fun renameProject(projectId: String, displayName: String)

    /**
     * Point a Project at the folder it lives in now, after that folder was renamed or moved. The Project keeps its id,
     * name and Tasks, and Task worktree locators inside the old folder follow it. A folder that belongs to another
     * Project or cannot be read changes nothing. The folders themselves are never touched.
     */
    fun relocateProject(projectId: String, rootPath: String): ProjectRelocation

    /** Create a plain-language Task (revision 1) under [projectId] and return it. */
    fun createTask(projectId: String, title: String, desiredOutcome: String): Task

    /** Record the Task's visible state, for example the user marking it done or reopening it. Unknown ids are ignored. */
    fun updateTaskStatus(taskId: String, status: TaskStatus)

    /**
     * Remember the agent a Task was started with (by name), or forget it with null, so it can be offered again after a
     * restart. It counts as the Task's most recent activity. Unknown ids are ignored.
     */
    fun setTaskAgent(taskId: String, agent: String?)

    /** Change a Task's title: trimmed, line breaks become spaces, cut to a bounded length. A blank title changes nothing. */
    fun renameTask(taskId: String, title: String)

    /**
     * Give Tasks new titles as upkeep rather than a user edit: like [renameTask], but a store that orders Tasks by
     * recent activity keeps their order, since nothing was done in them. Unknown ids are ignored.
     */
    fun retitleTasks(titles: Map<String, String>) {
        titles.forEach { (taskId, title) -> renameTask(taskId, title) }
    }

    /**
     * Remove a Task with its revisions and work context, and clear the selection and the Project's last selected
     * Task if they pointed at it. Its files and branch are not touched here. Unknown ids are ignored.
     */
    fun deleteTask(taskId: String)

    /**
     * Load the current-revision detail for [taskId] (request text and status), or
     * null if the Task or its revision is missing. Reads metadata only.
     */
    fun loadTask(taskId: String): TaskDetail?

    /** Persist the selected Project and Task so navigation restores after restart. */
    fun updateSelection(selectedProjectId: String?, selectedTaskId: String?)

    /** Load the persisted Work Context descriptor for [taskId], or null if none exists. */
    fun loadWorkContext(taskId: String): WorkContextDescriptor?

    /**
     * Persist bounded Work Context descriptors for a Task. This records lifecycle
     * and branch/worktree locators only; it never implies a live OS process.
     */
    fun saveWorkContext(descriptor: WorkContextDescriptor)
}
