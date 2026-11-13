package app.thdev.moru.feature.workbench

import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectRelocation
import app.thdev.moru.core.domain.TaskDetail
import app.thdev.moru.core.model.TaskStatus
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.core.model.WorkContextDescriptor
import app.thdev.moru.core.model.WorkContextPersistedLifecycle
import app.thdev.moru.feature.workbench.launch.AgentRuntime

/**
 * Stateful driver for the Projects & Tasks sidebar. It holds the current catalog
 * snapshot and the set of collapsed Projects, and turns sidebar interactions into
 * [ProjectCatalogRepository] calls. Construction loads the catalog once
 * (metadata only); every mutating method reloads and returns the new
 * [ProjectTaskSidebarState] so the hosting Composable can re-render.
 *
 * It owns no Compose state and no runtime children, which keeps it unit-testable.
 */
class ProjectTaskWorkbenchController(
    private val repository: ProjectCatalogRepository,
    /** The Projects the user left collapsed last time; every other Project starts expanded. */
    initiallyCollapsed: Set<String> = emptySet(),
    initialGeneral: GeneralSettings = GeneralSettings(),
) {
    private var general = initialGeneral
    private val collapsedProjectIds = initiallyCollapsed.toMutableSet()
    private var snapshot = repository.loadCatalog()

    init {
        snapshot.selectedProjectId?.let(::expandSelectedProject)
        numberAgentNamedTasks()
    }

    /** Rename the Tasks earlier versions named after their first agent, once, keeping their order. */
    private fun numberAgentNamedTasks() {
        val agentNames = AgentRuntime.entries.map { runtime -> runtime.displayName }
        val titles = snapshot.projects.flatMap { entry ->
            agentNamedTaskTitles(entry.project.displayName.ifBlank { "Untitled" }, entry.tasks, agentNames).entries
        }.associate { (taskId, title) -> taskId to title }
        if (titles.isEmpty()) return
        repository.retitleTasks(titles)
        snapshot = repository.loadCatalog()
    }

    /** The Projects the user collapsed, so the next launch can collapse them again. */
    fun collapsedProjects(): Set<String> = collapsedProjectIds.toSet()

    fun currentState(): ProjectTaskSidebarState =
        projectTaskSidebarState(snapshot, collapsedProjectIds.toSet(), general)

    /** Apply display preferences without touching the catalog or hydrating runtime children. */
    fun applyGeneralSettings(settings: GeneralSettings): ProjectTaskSidebarState {
        val wasAutoCollapse = general.autoCollapseInactiveSidebar
        general = settings
        if (!wasAutoCollapse && settings.autoCollapseInactiveSidebar) {
            snapshot.selectedProjectId?.let(::expandSelectedProject)
        } else if (wasAutoCollapse && !settings.autoCollapseInactiveSidebar) {
            collapsedProjectIds.clear()
        }
        return currentState()
    }

    /** Load the request text and status for a selected Task, or null if it is gone. */
    fun taskDetail(taskId: String): TaskDetail? = repository.loadTask(taskId)

    fun refresh(): ProjectTaskSidebarState {
        snapshot = repository.loadCatalog()
        return currentState()
    }

    fun toggleProject(projectId: String): ProjectTaskSidebarState {
        if (!collapsedProjectIds.add(projectId)) {
            collapsedProjectIds.remove(projectId)
        }
        return currentState()
    }

    /** Close a Project: it leaves the list but keeps its Tasks, and opening its folder again restores it. */
    fun closeProject(projectId: String): ProjectTaskSidebarState {
        collapsedProjectIds.remove(projectId)
        repository.closeProject(projectId)
        return refresh()
    }

    /** Delete a closed Project and its Tasks from the catalog; the folder is untouched. */
    fun forgetClosedProject(projectId: String): ProjectTaskSidebarState {
        repository.forgetClosedProject(projectId)
        return refresh()
    }

    /** Change a Project's display name; the folder is untouched. */
    fun renameProject(projectId: String, displayName: String): ProjectTaskSidebarState {
        repository.renameProject(projectId, displayName)
        return refresh()
    }

    /** Point a Project at the folder it was renamed or moved to; its Tasks stay. */
    fun relocateProject(projectId: String, rootPath: String): ProjectRelocation =
        repository.relocateProject(projectId, rootPath).also { refresh() }

    /** The user's decision that a Task is finished; a finished Task is no longer guessed from terminal output. */
    fun markTaskDone(taskId: String): ProjectTaskSidebarState {
        repository.updateTaskStatus(taskId, TaskStatus.Done)
        return refresh()
    }

    /** Puts a finished Task back to work: its state follows its terminals again. */
    fun reopenTask(taskId: String): ProjectTaskSidebarState {
        repository.updateTaskStatus(taskId, TaskStatus.Understanding)
        return refresh()
    }

    /** Remember the agent a Task was started with, so a restart can offer it first. */
    fun recordTaskAgent(taskId: String, agent: String): ProjectTaskSidebarState {
        repository.setTaskAgent(taskId, agent)
        return refresh()
    }

    /**
     * Names each numbered Task after its first session's summary (keyed by Task id), keeping the Tasks' order.
     * Null when no Task changes, so the caller can skip a redraw.
     */
    fun nameTasksFromSessions(firstSessionSummaries: Map<String, String>): ProjectTaskSidebarState? {
        val titles = snapshot.projects.flatMap { entry ->
            sessionNamedTaskTitles(entry.project.displayName.ifBlank { "Untitled" }, entry.tasks, firstSessionSummaries).entries
        }.associate { (taskId, title) -> taskId to title }
        if (titles.isEmpty()) return null
        repository.retitleTasks(titles)
        return refresh()
    }

    fun renameTask(taskId: String, title: String): ProjectTaskSidebarState {
        repository.renameTask(taskId, title)
        return refresh()
    }

    /** Removes the Task from the catalog; the caller has already dealt with its terminals and folder. */
    fun deleteTask(taskId: String): ProjectTaskSidebarState {
        repository.deleteTask(taskId)
        return refresh()
    }

    private fun expandSelectedProject(projectId: String) {
        if (general.autoCollapseInactiveSidebar) {
            snapshot.projects.forEach { entry ->
                if (entry.project.id == projectId) {
                    collapsedProjectIds.remove(entry.project.id)
                } else {
                    collapsedProjectIds.add(entry.project.id)
                }
            }
        } else {
            collapsedProjectIds.remove(projectId)
        }
    }

    fun selectProject(projectId: String): ProjectTaskSidebarState {
        expandSelectedProject(projectId)
        val lastTaskId = snapshot.projects
            .firstOrNull { entry -> entry.project.id == projectId }
            ?.project
            ?.lastSelectedTaskId
        repository.updateSelection(projectId, lastTaskId)
        return refresh()
    }

    fun selectTask(projectId: String, taskId: String): ProjectTaskSidebarState {
        expandSelectedProject(projectId)
        repository.updateSelection(projectId, taskId)
        openWorkContext(taskId)
        return refresh()
    }

    /**
     * Record that a Task has an active Work Context (its terminal/context is
     * open). Persists a bounded `Hibernated` descriptor; it never implies a live
     * process. Idempotent per Task.
     */
    private fun openWorkContext(taskId: String) {
        if (repository.loadWorkContext(taskId) == null) {
            repository.saveWorkContext(
                WorkContextDescriptor(
                    id = "wc-$taskId",
                    taskId = taskId,
                    persistedLifecycle = WorkContextPersistedLifecycle.Hibernated,
                ),
            )
        }
    }

    /** Register a Project for [rootPath]; the display name is derived from the folder. */
    fun openProject(rootPath: String): ProjectTaskSidebarState {
        val registration = repository.registerProject(displayName = "", rootPath = rootPath)
        collapsedProjectIds.remove(registration.project.id)
        repository.updateSelection(registration.project.id, registration.project.lastSelectedTaskId)
        return refresh()
    }

    fun createTask(projectId: String, title: String, desiredOutcome: String): ProjectTaskSidebarState {
        val task = repository.createTask(projectId, title, desiredOutcome)
        collapsedProjectIds.remove(projectId)
        repository.updateSelection(projectId, task.id)
        openWorkContext(task.id)
        return refresh()
    }

    /**
     * Prepare to compose a new Task under [projectId]: expand it and clear the Task
     * selection so the detail pane shows the composer instead of a Task.
     */
    fun startNewTask(projectId: String): ProjectTaskSidebarState {
        collapsedProjectIds.remove(projectId)
        repository.updateSelection(projectId, null)
        return refresh()
    }

    /**
     * Create a Task from a plain-language description. The first non-blank line
     * becomes the title (bounded); the full text is the desired outcome. A blank
     * description, which a Task started in a terminal may have, is numbered after its
     * Project ("Moru task 3").
     */
    fun createTaskFromDescription(projectId: String, description: String): ProjectTaskSidebarState {
        val trimmed = description.trim()
        val title = trimmed
            .lineSequence()
            .firstOrNull { line -> line.isNotBlank() }
            ?.trim()
            ?.take(80)
            ?.takeIf { candidate -> candidate.isNotEmpty() }
            ?: numberedTitle(projectId)
        return createTask(projectId, title, trimmed)
    }

    private fun numberedTitle(projectId: String): String {
        val entry = snapshot.projects.firstOrNull { candidate -> candidate.project.id == projectId }
        return nextNumberedTaskTitle(
            projectName = entry?.project?.displayName.orEmpty().ifBlank { "Untitled" },
            existingTitles = entry?.tasks.orEmpty().map { task -> task.title },
        )
    }
}
