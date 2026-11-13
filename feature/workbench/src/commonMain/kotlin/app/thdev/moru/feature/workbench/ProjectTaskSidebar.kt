package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.scroll.StableScrollColumn
import app.thdev.moru.core.model.session.AgentSessionRow
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.terminal.SessionPermissions
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.feature.workbench.sidebar.ClosedProjectsSection
import app.thdev.moru.feature.workbench.sidebar.NewTaskRow
import app.thdev.moru.feature.workbench.sidebar.ProjectGroup
import app.thdev.moru.feature.workbench.sidebar.SidebarDragAutoScroll
import app.thdev.moru.feature.workbench.sidebar.SidebarDragHandle
import app.thdev.moru.feature.workbench.sidebar.SidebarDragItem
import app.thdev.moru.feature.workbench.sidebar.SidebarDragScope
import app.thdev.moru.feature.workbench.sidebar.SidebarDragState
import app.thdev.moru.feature.workbench.sidebar.SidebarDragTarget
import app.thdev.moru.feature.workbench.sidebar.SidebarEmptyState
import app.thdev.moru.feature.workbench.sidebar.SidebarHeader
import app.thdev.moru.feature.workbench.sidebar.SidebarSessionSummary
import app.thdev.moru.feature.workbench.sidebar.SolidSidebar
import app.thdev.moru.feature.workbench.sidebar.SidebarViewSwitcher
import app.thdev.moru.feature.workbench.sidebar.TaskSessionGroup
import app.thdev.moru.feature.workbench.slots.ProjectActivitySpec
import app.thdev.moru.feature.workbench.slots.SessionBoardSpec
import app.thdev.moru.feature.workbench.slots.SessionsPopoverSpec
import app.thdev.moru.feature.workbench.slots.UnattachedSessionsSpec
import app.thdev.moru.feature.workbench.slots.WorkbenchSessionSlots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.launch.NewSessionPopover

/**
 * The persistent sidebar (the default left pane): every Project, the Tasks inside it and the sessions inside each
 * Task. A Task only says which group its sessions belong to; each session carries its own state. It is a stateless
 * surface driven by [ProjectTaskSidebarState]; all navigation and mutation flow back through the callbacks so the
 * hosting screen owns state. The header opens a popover with every session across all Projects. The sessions are
 * drawn by [sessionSlots].
 */
@Composable
fun ProjectTaskSidebar(
    state: ProjectTaskSidebarState,
    onOpenProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onToggleProject: (String) -> Unit,
    onSelectTask: (projectId: String, taskId: String) -> Unit,
    onNewTask: (projectId: String) -> Unit,
    modifier: Modifier = Modifier,
    sessionSlots: WorkbenchSessionSlots,
    onStartNewTask: ((projectId: String, runtime: AgentRuntime) -> Unit)? = null,
    newTaskRequestedProjectId: String? = null,
    onNewTaskRequestHandled: () -> Unit = {},
    projectRevealRequest: String? = null,
    onProjectRevealHandled: () -> Unit = {},
    onCloseProject: (String) -> Unit = {},
    runningSessions: Map<String, Int> = emptyMap(),
    onReopenProject: (rootPath: String) -> Unit = {},
    onForgetClosedProject: (projectId: String) -> Unit = {},
    onRenameProject: (projectId: String, displayName: String) -> Unit = { _, _ -> },
    /** Asks where a Project's folder is now, after it was renamed or moved. */
    onRelocateProject: (projectId: String) -> Unit = {},
    taskActions: TaskRowActions = TaskRowActions(),
    sessions: List<AgentSessionRow> = emptyList(),
    onSelectSession: (row: AgentSessionRow, projectId: String?) -> Unit = { _, _ -> },
    onOpenPreview: (sessionId: String, url: String) -> Unit = { _, _ -> },
    footer: @Composable () -> Unit = {},
    onReorderProjects: ((List<String>) -> Unit)? = null,
    onReorderTasks: ((projectId: String, orderedTaskIds: List<String>) -> Unit)? = null,
    /** Project and session navigation stays in the sidebar; embeddings may hide its header. */
    showHeader: Boolean = true,
    /** The view on top of the sidebar, Project tree or status board; null offers no switch and shows the tree. */
    view: SidebarView? = null,
    onViewChange: (SidebarView) -> Unit = {},
) = SolidSidebar {
    val projectReorder = rememberUpdatedState(onReorderProjects)
    val taskReorder = rememberUpdatedState(onReorderTasks)
    val dragState = remember {
        SidebarDragState { scope, ids ->
            val projectId = scope.projectId
            if (projectId == null) projectReorder.value?.invoke(ids) else taskReorder.value?.invoke(projectId, ids)
        }
    }
    SideEffect {
        dragState.configure(buildMap {
            if (onReorderProjects != null) put(SidebarDragScope(), state.projects.map { it.id })
            if (onReorderTasks != null) state.projects.forEach { put(SidebarDragScope(it.id), it.tasks.map { task -> task.id }) }
        })
    }
    var activeNewTaskProjectId by remember { mutableStateOf<String?>(null) }
    var headerNewTaskProjectId by remember { mutableStateOf<String?>(null) }
    var newTaskError by remember { mutableStateOf<String?>(null) }
    var reordering by remember { mutableStateOf(false) }
    val canReorder = view != SidebarView.Board && (
        (onReorderProjects != null && state.projects.size > 1) ||
            (onReorderTasks != null && state.projects.any { it.tasks.size > 1 })
        )
    if (!canReorder && reordering) {
        reordering = false
    }
    val pendingProjectReveal = newTaskRequestedProjectId ?: projectRevealRequest
    var viewportTop by remember { mutableFloatStateOf(0f) }
    val scroll = rememberScrollState()
    SidebarDragAutoScroll(dragState, scroll)
    val strings = workbenchStrings()
    val permissionRequests by SessionPermissions.requests.collectAsState()
    val taskRefs = state.projects
        .flatMap { project -> project.tasks.map { task -> task.id to SessionTaskRef(task.title, project.id, project.displayName) } }
        .toMap()
    val toggleReorder: (() -> Unit)? = if (canReorder) {
        {
            if (reordering) dragState.cancel()
            reordering = !reordering
        }
    } else null
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MoruTheme.colors.background),
    ) {
        if (showHeader) SidebarHeader(
            onOpenProject = onOpenProject,
            sessions = {
                sessionSlots.popover(
                    SessionsPopoverSpec(
                        rows = sessions,
                        tasks = taskRefs,
                        selectedProjectId = state.selectedProjectId,
                        onSelectSession = onSelectSession,
                        onOpenPreview = onOpenPreview,
                    ),
                )
            },
            reordering = reordering,
            onToggleReorder = toggleReorder,
        )
        view?.let { current ->
            SidebarViewSwitcher(
                view = current,
                onViewChange = onViewChange,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                reordering = reordering,
                onToggleReorder = toggleReorder.takeIf { !showHeader },
            )
        }

        if (view == SidebarView.Board) {
            sessionSlots.board(
                SessionBoardSpec(sessions, taskRefs, onSelectSession, onOpenPreview),
                Modifier.weight(1f),
            )
        } else if (state.isEmpty) {
            SidebarEmptyState()
        } else {
            StableScrollColumn(
                scroll = scroll,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("sidebar-viewport")
                    .onGloballyPositioned { coordinates ->
                        val origin = coordinates.positionInRoot()
                        viewportTop = origin.y
                        dragState.setViewport(Rect(origin.x, origin.y, origin.x + coordinates.size.width, origin.y + coordinates.size.height))
                    },
            ) {
                state.projects.forEachIndexed { index, project ->
                    key(project.id) {
                        val projectItem = SidebarDragItem(SidebarDragScope(), project.id)
                        val dismissNewTask = {
                            if (activeNewTaskProjectId == project.id) {
                                activeNewTaskProjectId = null
                                headerNewTaskProjectId = null
                                newTaskError = null
                            }
                        }
                        val startTask: (AgentRuntime) -> Unit = { runtime ->
                            dismissNewTask()
                            if (onStartNewTask != null) onStartNewTask(project.id, runtime) else onNewTask(project.id)
                        }
                        val hasSelectedTaskInProject = project.tasks.any { it.selected }
                        SidebarDragTarget(dragState, projectItem) {
                            ProjectGroup(
                                first = index == 0,
                                expanded = project.expanded,
                                // Keep primary row selection on its owning row while retaining active project framing.
                                selected = project.selected && (!project.expanded || !hasSelectedTaskInProject),
                                active = project.selected,
                                accentColor = app.thdev.moru.core.ui.identity.projectIdentityAccent(project.displayName)
                                    .color(MoruTheme.colors),
                                header = {
                                    Box(
                                        Modifier.revealSidebarSelection(
                                            project.selected && (!project.expanded || !hasSelectedTaskInProject),
                                            enabled = pendingProjectReveal == null,
                                        ).revealSidebarProject(
                                            requested = pendingProjectReveal == project.id,
                                            scroll = scroll,
                                            viewportTop = viewportTop,
                                            onHandled = {
                                                if (newTaskRequestedProjectId == project.id) {
                                                    headerNewTaskProjectId = project.id
                                                    activeNewTaskProjectId = project.id
                                                    newTaskError = null
                                                    onNewTaskRequestHandled()
                                                }
                                                if (projectRevealRequest == project.id) onProjectRevealHandled()
                                            },
                                        ),
                                    ) {
                                        ProjectRowView(
                                            project = project,
                                            onSelect = { onSelectProject(project.id) },
                                            onToggle = { onToggleProject(project.id) },
                                            onClose = { onCloseProject(project.id) },
                                            runningSessions = runningSessions[project.id] ?: 0,
                                            sessionSummary = SidebarSessionSummary.from(
                                                sessions.filter { taskRefs[it.taskKey]?.projectId == project.id },
                                                permissionRequests.keys,
                                            ),
                                            taskAttentionCount = SidebarSessionSummary.unrepresentedTaskAttention(
                                                project.tasks, sessions, permissionRequests.keys,
                                            ),
                                            onRename = { name -> onRenameProject(project.id, name) },
                                            onRelocate = { onRelocateProject(project.id) },
                                            onOpenInWindow = taskActions.onOpenProjectWindow
                                                ?.takeIf { project.isAvailable }
                                                ?.let { open -> { open(project.id) } },
                                            reorderHandle = if (reordering && onReorderProjects != null && state.projects.size > 1) {
                                                { SidebarDragHandle(dragState, projectItem, strings.reorderProject(project.displayName)) }
                                            } else null,
                                        )
                                        NewSessionPopover(
                                            expanded = activeNewTaskProjectId == project.id && headerNewTaskProjectId == project.id,
                                            errorMessage = newTaskError,
                                            onStart = startTask,
                                            onDismiss = dismissNewTask,
                                        )
                                    }
                                },
                                collapsedContent = {
                                    sessionSlots.projectActivity(
                                        ProjectActivitySpec(
                                            rows = sessions.filter { row -> taskRefs[row.taskKey]?.projectId == project.id },
                                            tasks = taskRefs,
                                            onSelectSession = { row -> onSelectSession(row, project.id) },
                                        ),
                                        Modifier,
                                    )
                                },
                                footer = {
                                    if (project.isAvailable) {
                                        NewTaskRow(
                                            projectName = project.displayName,
                                            expanded = activeNewTaskProjectId == project.id && headerNewTaskProjectId != project.id,
                                            errorMessage = if (activeNewTaskProjectId == project.id) newTaskError else null,
                                            onStart = startTask,
                                            onDismiss = dismissNewTask,
                                            onClick = {
                                                activeNewTaskProjectId = project.id
                                                headerNewTaskProjectId = null
                                                newTaskError = null
                                            },
                                        )
                                    }
                                },
                            ) {
                                project.tasks.forEachIndexed { taskIndex, task ->
                                    key(task.id) {
                                        val taskItem = SidebarDragItem(SidebarDragScope(project.id), task.id)
                                        SidebarDragTarget(dragState, taskItem) {
                                            TaskSessionGroup(
                                                task = task,
                                                sessions = sessions.filter { row -> row.taskKey == task.id },
                                                onSelectTask = { onSelectTask(project.id, task.id) },
                                                onSelectSession = { onSelectSession(it, project.id) },
                                                taskActions = taskActions,
                                                onOpenPreview = onOpenPreview,
                                                sessionRow = sessionSlots.row,
                                                first = taskIndex == 0,
                                                revealSelection = pendingProjectReveal == null,
                                                collapseIdleSessions = state.autoCollapseInactiveSidebar && hasSelectedTaskInProject && !task.selected,
                                                reorderHandle = if (reordering && onReorderTasks != null && project.tasks.size > 1) {
                                                    { SidebarDragHandle(dragState, taskItem, strings.reorderTask(task.title)) }
                                                } else null,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                sessionSlots.unattached(UnattachedSessionsSpec(sessions.filter { row -> row.taskKey !in taskRefs }, onSelectSession, onOpenPreview))
                ClosedProjectsSection(state.closedProjects, onReopenProject, onForgetClosedProject)
            }
        }
        footer()
    }
}
