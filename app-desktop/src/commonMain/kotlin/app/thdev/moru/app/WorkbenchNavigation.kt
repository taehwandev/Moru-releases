package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.app.i18n.AppStrings
import app.thdev.moru.app.i18n.EnglishAppStrings
import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectRelocation
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.workbench.ProjectTaskWorkbenchController
import app.thdev.moru.feature.workbench.TaskDisplayState
import app.thdev.moru.feature.workbench.TaskGroup
import app.thdev.moru.core.model.TaskAttention
import app.thdev.moru.core.model.GeneralSettings
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.agentLaunchCommand

/** Selection is independent of captured launches; changing views never retargets a Task. */
internal class WorkbenchNavigation(
    repository: ProjectCatalogRepository,
    initialNotice: String? = null,
    initiallyCollapsed: Set<String> = emptySet(),
    initialGeneral: GeneralSettings = GeneralSettings(),
    /** The text of the language in use now, read when a notice is written, so a switch is picked up. */
    private val strings: () -> AppStrings = { EnglishAppStrings },
) {
    private val controller = ProjectTaskWorkbenchController(repository, initiallyCollapsed, initialGeneral)

    /** The Projects the user collapsed, for the saved layout. */
    fun collapsedProjects(): Set<String> = controller.collapsedProjects()
    var sidebar by mutableStateOf(controller.currentState())
        private set
    var tab by mutableStateOf(ProjectTab.Work)
    var composingProjectId by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf(initialNotice)

    /** The Task whose terminal pane should open its session chooser, set by Cmd+T and cleared once the pane has. */
    var newSessionRequest by mutableStateOf<String?>(null)

    /** The Project whose sidebar row should open its task chooser, set by Cmd+N and cleared once the sidebar has. */
    var newTaskRequest by mutableStateOf<String?>(null)

    /** An explicit Project selection reveals its header even when its last Task is restored. */
    var projectRevealRequest by mutableStateOf<String?>(null)

    /** The Project whose usage the Work view's Result should open, set from the sidebar and cleared once it has. */
    var usageRequest by mutableStateOf<String?>(null)
    var preparingTaskIds by mutableStateOf(emptySet<String>())
        private set
    var failedLaunches by mutableStateOf(emptyMap<String, Launch>())
        private set
    private var executionFolders by mutableStateOf(emptyMap<String, String>())

    val selectedProject get() = sidebar.projects.firstOrNull { it.id == sidebar.selectedProjectId }
    val selectedTask get() = selectedProject?.tasks?.firstOrNull { it.id == sidebar.selectedTaskId }
    fun applyGeneralSettings(settings: GeneralSettings) { sidebar = controller.applyGeneralSettings(settings) }
    /** Reads the catalog again, after another window changed it. */
    fun reload() { sidebar = controller.refresh() }
    fun taskDescription(taskId: String) = controller.taskDetail(taskId)?.desiredOutcome

    fun executionFolder(taskId: String?, sessions: TerminalSessionManager): String? = taskId?.let { id ->
        executionFolders[id] ?: sessions.listSessions().firstOrNull {
            it.spec.attachedItemId == id && it.spec.kind.isTaskSession
        }?.spec?.workingDirectory
    }

    fun launchPresentation(state: app.thdev.moru.feature.workbench.ProjectTaskSidebarState) = state.copy(
        projects = state.projects.map { project -> project.copy(tasks = project.tasks.map { task ->
            when (task.id) {
                in failedLaunches -> task.copy(displayState = TaskDisplayState.Failed,
                    group = TaskGroup.NeedsAttention, attention = TaskAttention.Failed)
                in preparingTaskIds -> task.copy(displayState = TaskDisplayState.Preparing,
                    group = TaskGroup.Running)
                else -> task
            }
        }) },
    )

    fun openProject(path: String) {
        runCatching { controller.openProject(path) }
            .onSuccess { sidebar = it; projectRevealRequest = null; composingProjectId = null; tab = ProjectTab.Work; notice = null }
            .onFailure { notice = strings().let { text -> text.couldNotOpenProject(it.message ?: text.unknownError) } }
    }

    fun selectProject(id: String) {
        sidebar = controller.selectProject(id)
        projectRevealRequest = id
        composingProjectId = null
        tab = ProjectTab.Work
    }

    /**
     * Opens [id]'s usage beside the current view, selecting that Project first when another one is open. Every view
     * keeps the tool strip, so Code and Git stay where they are.
     */
    fun showProjectUsage(id: String) {
        if (sidebar.selectedProjectId != id) selectProject(id)
        usageRequest = id
    }

    /** Selects a Task in the view already open, so moving between Tasks keeps Code or Git on screen. */
    fun selectTask(projectId: String, taskId: String) {
        projectRevealRequest = null
        sidebar = controller.selectTask(projectId, taskId)
        composingProjectId = null
    }

    /** Closes a Project: it leaves the list and its Tasks are kept. The host stops its sessions first. */
    fun closeProject(id: String) {
        if (projectRevealRequest == id) projectRevealRequest = null
        if (composingProjectId == id) composingProjectId = null
        sidebar = controller.closeProject(id)
        tab = ProjectTab.Work
    }

    /** Deletes a closed Project from the archive with its Tasks; its folder is untouched. */
    fun forgetClosedProject(id: String) {
        sidebar = controller.forgetClosedProject(id)
    }

    fun renameProject(id: String, displayName: String) {
        sidebar = controller.renameProject(id, displayName)
    }

    /** Points Project [id] at the folder it was renamed or moved to, keeping its Tasks. */
    fun relocateProject(id: String, path: String) {
        val outcome = runCatching { controller.relocateProject(id, path) }
            .getOrElse { error -> notice = strings().let { text -> text.couldNotOpenProject(error.message ?: text.unknownError) }; return }
        sidebar = controller.currentState()
        notice = when (outcome) {
            ProjectRelocation.Moved, ProjectRelocation.NotFound -> null
            ProjectRelocation.FolderInUse -> strings().projectFolderInUse
            ProjectRelocation.FolderUnavailable -> strings().projectFolderUnavailable
        }
    }

    /**
     * Selects the Task [step] places after the selected one in the open project, wrapping round the ends; with none
     * selected, forward starts at the first and backward at the last. Does nothing when the project has no Tasks.
     */
    fun selectAdjacentTask(step: Int) {
        val project = selectedProject ?: return
        val tasks = project.tasks
        if (tasks.isEmpty()) return
        val current = tasks.indexOfFirst { task -> task.id == sidebar.selectedTaskId }
        val target = if (current < 0) {
            if (step >= 0) 0 else tasks.lastIndex
        } else {
            Math.floorMod(current + step, tasks.size)
        }
        selectTask(project.id, tasks[target].id)
    }

    /** Selects the Project [step] places after the selected one in sidebar order, wrapping round the ends. */
    fun selectAdjacentProject(step: Int) {
        val projects = sidebar.projects
        if (projects.isEmpty()) return
        val current = projects.indexOfFirst { project -> project.id == sidebar.selectedProjectId }
        val target = if (current < 0) {
            if (step >= 0) 0 else projects.lastIndex
        } else {
            Math.floorMod(current + step, projects.size)
        }
        selectProject(projects[target].id)
    }

    fun markTaskDone(id: String) { sidebar = controller.markTaskDone(id) }

    fun reopenTask(id: String) { sidebar = controller.reopenTask(id) }

    fun renameTask(id: String, title: String) { sidebar = controller.renameTask(id, title) }

    fun nameTasksFromSessions(firstSessionSummaries: Map<String, String>) {
        controller.nameTasksFromSessions(firstSessionSummaries)?.let { sidebar = it }
    }

    fun deleteTask(id: String) { sidebar = controller.deleteTask(id) }

    fun toggleProject(id: String) { sidebar = controller.toggleProject(id) }

    fun newTask(projectId: String) {
        sidebar = controller.startNewTask(projectId)
        composingProjectId = projectId
        tab = ProjectTab.Work
    }

    fun cancelComposition() { composingProjectId = null }

    fun launchDirect(projectId: String, runtime: AgentRuntime): Launch? {
        val project = sidebar.projects.firstOrNull { it.id == projectId } ?: return null
        val root = project.rootPath ?: return null
        if (!project.isAvailable) return null
        composingProjectId = null
        sidebar = controller.createTaskFromDescription(project.id, "")
        tab = ProjectTab.Work
        val task = selectedTask ?: return null
        return Launch(project.id, task.id, task.title, root, "", runtime)
    }

    fun beginLaunch(description: String, runtime: AgentRuntime): Launch? {
        val project = selectedProject ?: return null
        val root = project.rootPath ?: return null
        if (composingProjectId != project.id || !project.isAvailable) return null
        // Consume this composer before any second click or keyboard submission can create a Task.
        composingProjectId = null
        sidebar = controller.createTaskFromDescription(project.id, description)
        val task = selectedTask ?: return null
        return Launch(project.id, task.id, task.title, root, description, runtime)
    }

    fun prepare(launch: Launch): Boolean {
        if (launch.taskId in preparingTaskIds || launch.taskId in executionFolders) return false
        preparingTaskIds = preparingTaskIds + launch.taskId
        failedLaunches = failedLaunches - launch.taskId
        return true
    }

    fun completeLaunch(
        launch: Launch,
        sessions: TerminalSessionManager,
        shell: String,
        startTerminal: (TerminalSessionSnapshot) -> Unit = {},
    ) {
        preparingTaskIds = preparingTaskIds - launch.taskId
        var createdSession: TerminalSessionSnapshot? = null
        runCatching {
            sessions.createTaskTerminalSession(
                TerminalSessionSpec(
                    title = launch.runtime.displayName,
                    shell = shell,
                    workingDirectory = launch.projectRoot,
                    attachedItemId = launch.taskId,
                    startupCommand = agentLaunchCommand(launch.runtime, launch.description),
                ),
            ).also { created ->
                createdSession = created
                startTerminal(created)
            }
        }.onSuccess {
            executionFolders = executionFolders + (launch.taskId to it.spec.workingDirectory)
            notice = null
            // Remembering the agent is a convenience; failing to store it must not undo a launch that worked.
            runCatching { sidebar = controller.recordTaskAgent(launch.taskId, launch.runtime.name) }
        }.onFailure {
            createdSession?.let { created -> runCatching { sessions.closeSession(created.id) } }
            failedLaunches = failedLaunches + (launch.taskId to launch)
            notice = strings().let { text -> text.couldNotStart(launch.title, it.message ?: text.unknownError) }
        }
    }

    data class Launch(
        val projectId: String,
        val taskId: String,
        val title: String,
        val projectRoot: String,
        val description: String,
        val runtime: AgentRuntime,
    )
}
