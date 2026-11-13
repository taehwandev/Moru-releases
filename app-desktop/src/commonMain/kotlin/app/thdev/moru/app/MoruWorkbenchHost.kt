package app.thdev.moru.app

import app.thdev.moru.feature.workbench.slots.SessionBoardSpec
import app.thdev.moru.core.model.session.SessionTaskRef
import app.thdev.moru.core.model.session.AgentSessionRow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.terminal.SessionLastLines
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.terminal.TaskTerminalSessionCapacity.canAddTaskTerminalSession
import app.thdev.moru.core.ui.shortcut.label
import app.thdev.moru.feature.sessions.rememberAgentSessionRows
import app.thdev.moru.feature.sessions.rememberTaskSessionActivities
import app.thdev.moru.feature.sessions.taskLastLines
import app.thdev.moru.feature.terminal.rememberTerminalRevision
import app.thdev.moru.feature.terminal.TerminalSplitRequests
import app.thdev.moru.feature.workbench.beside.BesideTaskIds
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.terminal.requestTerminalFocus
import app.thdev.moru.core.model.panel.PanelEdge
import app.thdev.moru.feature.workbench.*
import app.thdev.moru.feature.workbench.restore.ClosedTerminals
import app.thdev.moru.core.terminal.TerminalSessionKind
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import app.thdev.moru.feature.workbench.statusbar.WorkbenchStatusBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The integration owner for Project selection, Task launch, and their scoped result views. */
@Composable
internal fun MoruWorkbenchHost(
    navigation: WorkbenchNavigation,
    bindings: MoruWorkbenchBindings,
    appScope: CoroutineScope,
    fallbackRoot: String,
    sessions: TerminalSessionManager,
    terminalWorkspaceRepository: TerminalWorkspaceRepository,
    settings: AppSettings,
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit,
    commands: MoruCommands = MoruCommands(),
    showWorkbench: () -> Unit = {},
    layout: WorkbenchLayoutKeeper? = null,
    onReorderProjects: ((List<String>) -> Unit)? = null,
    onReorderTasks: ((String, List<String>) -> Unit)? = null,
    orderingFeedback: @Composable () -> Unit = {},
    /** The Projects open in windows of their own; see [ProjectWindows]. */
    projectWindows: ProjectWindows? = null,
    /** Whether this is the main window, which also does the work kept once for the whole app. */
    mainWindow: Boolean = true,
) {
    val strings = appStrings()
    val root = navigation.selectedProject?.rootPath ?: fallbackRoot
    val taskId = navigation.sidebar.selectedTaskId
    val viewFolder = navigation.executionFolder(taskId, sessions) ?: root
    val colors = MoruTheme.colors
    LaunchedEffect(root, navigation.sidebar.selectedProjectId) {
        if (navigation.selectedProject != null) bindings.selectProjectRoot(root)
    }
    LaunchedEffect(viewFolder) { bindings.viewFolderChanged(viewFolder) }
    val liveState = navigation.launchPresentation(
        navigation.sidebar.withSessionActivities(
            rememberTaskSessionActivities(sessions),
            taskLastLines(sessions.listSessions(), SessionLastLines::lineFor),
        ),
    )
    val openProject = {
        navigation.openProjectFromPicker(bindings.pickProjectFolder, strings)
        Unit
    }
    val launch: (WorkbenchNavigation.Launch) -> Unit = { captured ->
        if (navigation.prepare(captured)) appScope.launch {
            navigation.completeLaunch(captured, sessions, settings.terminal.shellCommand ?: "zsh") {
                bindings.startTerminal(it, settings, colors)
            }
        }
    }
    val closeProject: (String) -> Unit = { projectId ->
        // Its agents stop with it: a closed Project has no screen from which to reach them.
        val taskIds = navigation.sidebar.projects.firstOrNull { it.id == projectId }?.tasks?.map { it.id }.orEmpty().toSet()
        sessions.listSessions().filter { it.spec.attachedItemId in taskIds }.forEach { session ->
            endTerminalSession(session, sessions, bindings.terminateHeldTerminal)
        }
        navigation.closeProject(projectId)
    }
    val deleteTask: (String) -> Unit = { taskId ->
        navigation.deleteTaskWithSessions(taskId, sessions, bindings.terminateHeldTerminal)
    }
    val taskActions = TaskRowActions(
        onRename = navigation::renameTask,
        onDelete = deleteTask,
        besideTaskIds = setOfNotNull(
            BesideTaskIds.shown(layout?.layout?.besideTaskIds.orEmpty(), liveState.selectedTaskId, liveState.projects),
        ),
        besideProjectId = liveState.selectedProjectId,
        // Experimental, and from the main window only: a Project window holds one Project already.
        onOpenProjectWindow = projectWindows?.takeIf { settings.experimental.projectWindows && mainWindow }
            ?.let { windows -> windows::open },
        // An Experimental feature: without it the menu has no entry and nothing opens beside.
        onToggleBeside = layout?.takeIf { settings.experimental.tasksBeside }?.let { keeper ->
            { id ->
                keeper.update { saved -> saved.copy(besideTaskIds = BesideTaskIds.toggle(saved.besideTaskIds, id, liveState.projects)) }
            }
        },
    )
    val running = runningSessionsByProject(liveState, sessions.listSessions())
    // A Project window keeps the terminals too, but only the main window's sidebar says which Projects are collapsed.
    layout?.let { keeper -> KeepWorkbenchLayout(keeper, navigation, sessions, keepsCollapsed = mainWindow) }
    if (mainWindow) {
        ReattachHeldTerminals(sessions, bindings, settings, colors)
    }
    // Project windows are Experimental: while off, every Project shows here as before.
    val windows = projectWindows?.takeIf { settings.experimental.projectWindows }
    // Choosing in the main window a Project that has its own window brings that window forward.
    val selectedProjectId = navigation.sidebar.selectedProjectId
    LaunchedEffect(selectedProjectId) {
        if (mainWindow && selectedProjectId != null) windows?.bringForward(selectedProjectId)
    }
    // With more than one window, only the focused one makes its chosen terminal the app's selected session.
    val focused = LocalWindowInfo.current.isWindowFocused
    val publishesSelection = windows?.windows.isNullOrEmpty() || focused
    val terminalRevision by rememberTerminalRevision(sessions)
    var immediateSessionRevision by remember(sessions) { mutableLongStateOf(sessions.revision()) }
    val selectSession: (String) -> Unit = { sessionId ->
        sessions.selectSession(sessionId)
        // A direct selection updates rows in the same frame as Task navigation, including within the same Task.
        immediateSessionRevision = sessions.revision()
    }
    val allSessionRows = rememberAgentSessionRows(sessions, maxOf(terminalRevision, immediateSessionRevision))
    // A Task started without a description takes the summary of its first session that has one. The main window
    // sees every session, so it alone names them.
    val firstSessionSummaries = buildMap {
        allSessionRows.forEach { row ->
            val task = row.taskKey
            val summary = row.summary
            if (task != null && summary != null && task !in this) put(task, summary)
        }
    }
    LaunchedEffect(firstSessionSummaries, mainWindow) {
        if (mainWindow && firstSessionSummaries.isNotEmpty()) navigation.nameTasksFromSessions(firstSessionSummaries)
    }
    // A Project window lists its own Project's sessions only; the others belong to the main window.
    val sessionRows = if (mainWindow) allSessionRows else {
        val ownTasks = liveState.projects.flatMap { project -> project.tasks.map { it.id } }.toSet()
        allSessionRows.filter { row -> row.taskKey in ownTasks }
    }
    // A terminal closed with Cmd+W is kept with the layout, so Cmd+Shift+T opens it again, even after a restart.
    val terminalClose = remember(sessions, layout, bindings) {
        TerminalClose(sessions) { closed ->
            appScope.launch {
                layout?.update { saved -> saved.copy(closedTerminals = ClosedTerminals.add(saved.closedTerminals, closed)) }
                closeTerminalNow(sessions, closed.id)
            }
        }
    }
    // Drop questions for another Task, while keeping a session request that brought this view back from Settings.
    LaunchedEffect(taskId) {
        terminalClose.keep()
        if (navigation.newSessionRequest != taskId) navigation.newSessionRequest = null
    }
    val canCloseTerminal = terminalClose.hasTerminal(taskId)
    val liveTaskIds = navigation.sidebar.projects.flatMap { project -> project.tasks.map { it.id } }.toSet()
    val canReopenTerminal = layout?.layout?.closedTerminals?.let { closed ->
        ClosedTerminals.newestOpenable(closed) { it in liveTaskIds } != null
    } == true
    val canMoveBetweenSessions = taskTerminalIds(sessions, taskId).size > 1
    // The Task whose terminal last took the keyboard: the selected Task or the one beside it. ⌘D and ⌘T act there.
    val besideShown = BesideTaskIds.shown(layout?.layout?.besideTaskIds.orEmpty(), taskId, liveState.projects)
        .takeIf { settings.experimental.tasksBeside }
    var activeTerminal by remember { mutableStateOf<Pair<String, String>?>(null) }
    val shortcutTarget = ShortcutTarget.of(taskId, besideShown, activeTerminal)
    val keyboardTask = shortcutTarget.taskId
    val keyboardSession = shortcutTarget.sessionId
    val canAddSession = taskId != null && navigation.selectedProject?.isAvailable == true &&
        navigation.composingProjectId == null && sessions.canAddTaskTerminalSession(taskId)
    // Read while composing so the commands follow the selection; a read inside SideEffect would not be observed.
    val selected = navigation.selectedProject
    SideEffect {
        commands.closeTerminal = { showWorkbench(); terminalClose.request(taskId) }.takeIf { canCloseTerminal }
        commands.reopenClosedTerminal = layout?.let { keeper ->
            fun() {
                reopenClosedTerminal(keeper.layout.closedTerminals, sessions, liveTaskIds) { bindings.startTerminal(it, settings, colors) }
                    ?.let { (remaining, session) ->
                        keeper.update { saved -> saved.copy(closedTerminals = remaining) }
                        val reopenedTask = session.spec.attachedItemId
                        navigation.sidebar.projects.firstOrNull { project -> project.tasks.any { it.id == reopenedTask } }
                            ?.let { project -> if (reopenedTask != null) navigation.selectTask(project.id, reopenedTask) }
                        showWorkbench()
                        navigation.tab = ProjectTab.Work
                        requestTerminalFocus(session.id)
                    }
            }
        }.takeIf { canReopenTerminal }
        commands.newSession = {
            // The native menu can still hold the callback from the frame before a Task switch.
            val target = keyboardTask.takeIf { it != taskId } ?: navigation.sidebar.selectedTaskId
            if (target != null && sessions.canAddTaskTerminalSession(target)) {
                showWorkbench()
                navigation.tab = ProjectTab.Work
                navigation.newSessionRequest = target
            }
        }.takeIf { canAddSession }
        // A split opens a plain shell on that side of the selected terminal, as a terminal app's Command-D does.
        val split: (PanelEdge) -> Unit = { edge ->
            val selectedTaskId = keyboardTask.takeIf { it != taskId } ?: navigation.sidebar.selectedTaskId
            if (selectedTaskId != null && sessions.canAddTaskTerminalSession(selectedTaskId)) {
                val beside = (keyboardSession ?: sessions.selectedSessionId())
                    ?.takeIf { id -> sessions.listSessions().any { it.id == id && it.spec.attachedItemId == selectedTaskId } }
                showWorkbench()
                navigation.tab = ProjectTab.Work
                startAdditionalTaskSession(
                    sessions, selectedTaskId, navigation.executionFolder(selectedTaskId, sessions) ?: root,
                    settings.terminal.shellCommand?.takeIf(String::isNotBlank) ?: "zsh", AgentRuntime.Terminal,
                ) { session -> bindings.startTerminal(session, settings, colors) }.onSuccess { created ->
                    beside?.let { TerminalSplitRequests.place(created.id, it, edge) }
                    requestTerminalFocus(created.id)
                }
            }
        }
        commands.splitRight = { split(PanelEdge.Right) }.takeIf { canAddSession }
        commands.splitDown = { split(PanelEdge.Bottom) }.takeIf { canAddSession }
        // Moving between a Task's terminals shows them in Work, where its tabs are.
        val moveSession: (Int) -> Unit = { step ->
            adjacentTaskSession(sessions, navigation.sidebar.selectedTaskId, step)?.let { id ->
                showWorkbench()
                navigation.tab = ProjectTab.Work
                selectSession(id)
                ViewedSessionResponseObserver.requestOpening(id)
                requestTerminalFocus(id)
            }
        }
        commands.nextSession = { moveSession(1) }.takeIf { canMoveBetweenSessions }
        commands.previousSession = { moveSession(-1) }.takeIf { canMoveBetweenSessions }
        commands.openProject = { showWorkbench(); openProject() }
        commands.newTask = selected?.takeIf { it.isAvailable }?.let { project ->
            {
                showWorkbench()
                navigation.newTaskRequest = project.id
            }
        }
        commands.closeProject = selected?.let { project ->
            {
                val count = running[project.id] ?: 0
                // Stopping agents always goes through the confirm in the sidebar, never a bare shortcut.
                if (count == 0) closeProject(project.id) else navigation.notice = strings.closeNeedsConfirm(count)
            }
        }
    }
    var unkeptView by remember { mutableStateOf(SidebarView.Projects) }
    var unkeptSeen by remember { mutableStateOf(emptySet<Int>()) }
    val sidebarView = layout?.let { keeper -> SidebarView.entries.firstOrNull { it.name == keeper.layout.sidebarView } ?: SidebarView.Projects }
        ?: unkeptView
    val changeSidebarView: (SidebarView) -> Unit = { next -> layout?.update { it.copy(sidebarView = next.name) } ?: run { unkeptView = next } }
    val selectSessionRow: (AgentSessionRow, String?) -> Unit = { row, projectId ->
        selectSession(row.sessionId)
        // The session belongs to a Task; the rest of the app follows it there.
        row.taskKey?.let { key -> if (projectId != null) navigation.selectTask(projectId, key) }
        navigation.tab = ProjectTab.Work
        ViewedSessionResponseObserver.requestOpening(row.sessionId)
        requestTerminalFocus(row.sessionId)
    }
    val seenResults = SeenResults(
        seen = layout?.layout?.seenResults?.toSet() ?: unkeptSeen,
        markSeen = { key -> layout?.update { it.copy(seenResults = it.seenResults.plusSeen(key)) } ?: run { unkeptSeen = unkeptSeen + key } },
    )
    // The Moru view fills the middle with its page of results; going to a Task or a session leaves it for that Task.
    val showsResults = sidebarView == SidebarView.Results
    val leaveResults = { if (showsResults) changeSidebarView(SidebarView.Projects) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            WithSessionTaskMoves(navigation, sessions, maxOf(terminalRevision, immediateSessionRevision), layout,
                onMoved = { id -> immediateSessionRevision = sessions.revision(); requestTerminalFocus(id) }) {
            CompositionLocalProvider(LocalSeenResults provides seenResults) {
            ProjectTaskSidebar(
                state = liveState,
                onOpenProject = openProject,
                onSelectProject = navigation::selectProject,
                onToggleProject = navigation::toggleProject,
                onSelectTask = { projectId, taskId -> leaveResults(); navigation.selectTask(projectId, taskId) },
                onNewTask = navigation::newTask,
                modifier = Modifier.width(260.dp),
                sessionSlots = bindings.paneSlots.sessions,
                onStartNewTask = { projectId, runtime ->
                    navigation.launchDirect(projectId, runtime)?.let(launch)
                },
                newTaskRequestedProjectId = navigation.newTaskRequest,
                onNewTaskRequestHandled = { navigation.newTaskRequest = null },
                projectRevealRequest = navigation.projectRevealRequest,
                onProjectRevealHandled = { navigation.projectRevealRequest = null },
                onCloseProject = closeProject,
                onReopenProject = navigation::openProject,
                onForgetClosedProject = navigation::forgetClosedProject,
                onRenameProject = navigation::renameProject,
                onRelocateProject = { projectId -> navigation.relocateProjectFromPicker(projectId, bindings.pickFolder, strings) },
                taskActions = taskActions,
                runningSessions = runningSessionsByProject(liveState, sessions.listSessions()),
                sessions = sessionRows,
                onSelectSession = { row, projectId -> leaveResults(); selectSessionRow(row, projectId) },
                onOpenPreview = { _, _ -> },
                footer = { Column(Modifier.padding(8.dp)) { orderingFeedback() } },
                onReorderProjects = onReorderProjects,
                onReorderTasks = onReorderTasks,
                showHeader = false,
                view = sidebarView,
                onViewChange = changeSidebarView,
            )
            }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                navigation.notice?.let { NoticeBanner(it, onDismiss = { navigation.notice = null }) }
                terminalClose.pendingSessionId?.let { pending ->
                    // A modal alert in its own window, which dims everything behind it, native terminals included.
                    TerminalCloseConfirm(
                        onConfirm = terminalClose::confirm,
                        onKeepOpen = terminalClose::keep,
                        shortcut = commands.keyboard.chordFor(AppShortcut.CloseTerminal)?.label(),
                        title = sessions.listSessions().firstOrNull { it.id == pending }?.spec?.title,
                        reopenShortcut = commands.keyboard.chordFor(AppShortcut.ReopenClosedTerminal)?.label(),
                    )
                }
                val project = navigation.selectedProject
                if (showsResults) CompositionLocalProvider(LocalSeenResults provides seenResults) {
                    val taskRefs = liveState.projects.flatMap { item -> item.tasks.map { task -> task.id to SessionTaskRef(task.title, item.id, item.displayName) } }.toMap()
                    bindings.paneSlots.sessions.results(
                        SessionBoardSpec(sessionRows, taskRefs, { row, projectId -> leaveResults(); selectSessionRow(row, projectId) },
                            onOpenPreview = { _, _ -> }),
                        Modifier.weight(1f).fillMaxWidth(),
                    )
                } else if (project == null) {
                    WorkbenchWelcome(onOpenProject = openProject, onNewTask = {})
                } else {
                    val request = remember(navigation, taskId) { taskId?.let(navigation::taskDescription) }
                    if (!request.isNullOrBlank() && taskId != null) key(taskId) { TaskRequestPanel(request) }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        run {
                        WorkbenchSelectedSurface(
                            navigation, bindings, root, viewFolder, sessions, terminalWorkspaceRepository,
                            settings, liveState, onOpenExternalTerminal, openProject, launch,
                            layout = layout,
                            projectWindows = windows.takeIf { mainWindow },
                            publishesSelection = publishesSelection,
                            onTerminalActivated = { activatedTask, session ->
                                activeTerminal = activatedTask to session
                            },
                        )
                        }
                    }
                }
            }
        }
        WorkbenchStatusBar(
            waiting = liveState.waitingCount,
            working = liveState.runningCount,
            folder = viewFolder.takeIf { navigation.selectedProject != null },
            branch = bindings.gitStatus.branchName.takeIf {
                navigation.selectedProject != null && bindings.matchesFolder(viewFolder) && bindings.gitStatus.repositoryDetected
            },
        )
    }
}
