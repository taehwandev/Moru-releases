package app.thdev.moru.app

import app.thdev.moru.feature.workbench.launch.AgentRuntime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.effectiveScrollbackLines
import app.thdev.moru.core.model.effectiveTerminalFontFamily
import app.thdev.moru.core.model.effectiveTerminalFontSize
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.feature.sessions.rememberAgentSessionRows
import app.thdev.moru.feature.terminal.rememberTerminalRevision
import app.thdev.moru.feature.workbench.*
import app.thdev.moru.feature.workbench.launch.NewTaskComposer
import app.thdev.moru.feature.git.ProjectGitScreen
import app.thdev.moru.feature.workbench.result.ProjectToolsFrame
import app.thdev.moru.feature.workbench.slots.SessionBranchReader
import app.thdev.moru.feature.workbench.beside.BesideTask
import app.thdev.moru.feature.workbench.beside.BesideTaskIds

@Composable
internal fun WorkbenchSelectedSurface(
    navigation: WorkbenchNavigation,
    bindings: MoruWorkbenchBindings,
    projectRoot: String,
    viewFolder: String,
    sessions: TerminalSessionManager,
    terminalWorkspaceRepository: TerminalWorkspaceRepository,
    settings: AppSettings,
    liveState: ProjectTaskSidebarState,
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit,
    openProject: () -> Unit,
    launch: (WorkbenchNavigation.Launch) -> Unit,
    layout: WorkbenchLayoutKeeper? = null,
    /** The Projects open in windows of their own; the main window shows a note for those instead of their Tasks. */
    projectWindows: ProjectWindows? = null,
    /** Whether choosing a terminal here makes it the app's selected session; only the focused window does. */
    publishesSelection: Boolean = true,
    /** A terminal of the selected Task or of the Task beside took the keyboard; the window's shortcuts follow it. */
    onTerminalActivated: (taskId: String, sessionId: String) -> Unit = { _, _ -> },
) {
    val project = navigation.selectedProject ?: return
    val task = liveState.projects.flatMap { it.tasks }.firstOrNull { it.id == liveState.selectedTaskId }
    val colors = MoruTheme.colors
    val strings = appStrings()
    // A worktree the person picked in Git's branch list; Git stays there until the Task's own folder changes.
    var gitFolder by remember(project.id, viewFolder) { mutableStateOf<String?>(null) }
    // Code and Git replace only the left side; the right tool strip and any tool left open stay put across tabs.
    val tabContent: (@Composable () -> Unit)? = when {
        navigation.tab == ProjectTab.Code && !bindings.matchesFolder(viewFolder) -> {
            { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MoruText(appStrings().loadingProjectFiles) } }
        }
        navigation.tab == ProjectTab.Code -> { { key(viewFolder) { ProjectCodeScreen(
            workspacePath = viewFolder,
            entries = bindings.treeEntries,
            gitStatus = bindings.gitStatus,
            onRefreshGitStatus = bindings.refreshGitStatus,
            onOpenProjectFile = bindings.openFile,
            onSaveProjectFile = bindings.saveFile,
            onCreateProjectFile = bindings.createFile,
            onRenameProjectFile = bindings.renameFile,
            onDeleteProjectFile = bindings.deleteFile,
            onRefreshProjectTree = bindings.refreshTree,
            openFilePath = layout?.layout?.openFiles?.get(viewFolder),
            onOpenFileChange = { path ->
                layout?.update { saved ->
                    saved.copy(openFiles = if (path == null) saved.openFiles - viewFolder else saved.openFiles + (viewFolder to path))
                }
            },
            expandedFolders = layout?.layout?.expandedFolders?.get(viewFolder).orEmpty().toSet(),
            onExpandedFoldersChange = { folders ->
                layout?.update { saved ->
                    saved.copy(
                        expandedFolders = if (folders.isEmpty()) saved.expandedFolders - viewFolder
                            else saved.expandedFolders + (viewFolder to folders.sorted()),
                    )
                }
            },
            onLoadProjectFolder = bindings.loadFolder,
            explorerOptions = layout?.layout?.codeExplorer ?: CodeExplorerOptions(),
            onExplorerOptionsChange = { options -> layout?.update { saved -> saved.copy(codeExplorer = options) } },
        ) } } }
        navigation.tab == ProjectTab.Git -> { { key(gitFolder ?: viewFolder) {
            ProjectGitScreen(
                workspacePath = gitFolder ?: viewFolder,
                loadGitHistory = bindings.loadGitHistory,
                loadCommitDiff = bindings.loadCommitDiff,
                loadWorkingChanges = bindings.loadWorkingChanges,
                gitRepository = bindings.gitRepository,
                onRepositoryChanged = { bindings.refreshGitStatus(); bindings.refreshTree() },
                detailWidth = layout?.layout?.gitDetailWidthDp?.dp,
                onDetailWidthChange = { width -> layout?.update { saved -> saved.copy(gitDetailWidthDp = width?.value) } },
                onOpenWorktree = { path -> gitFolder = path.takeIf { it != viewFolder } },
            )
        } } }
        else -> null
    }
    val composing = navigation.composingProjectId == project.id
    val sessionsVisible = workbenchSessionSurfaceVisible(
        projectInOwnWindow = projectWindows?.isOpen(project.id) == true,
        projectAvailable = project.isAvailable,
        taskSelected = task != null,
        composing = composing,
        preparing = task?.id?.let { it in navigation.preparingTaskIds } == true,
        failedLaunch = task?.id?.let { it in navigation.failedLaunches } == true,
    )
    when {
        projectWindows?.isOpen(project.id) == true -> ProjectInOwnWindow(
            project.displayName,
            onBringForward = { projectWindows.bringForward(project.id) },
            onMoveHere = { projectWindows.close(project.id) },
        )
        !project.isAvailable -> WorkbenchUnavailableProject(
            project.displayName,
            onFindFolder = { navigation.relocateProjectFromPicker(project.id, bindings.pickFolder, strings) },
            onOpenProject = openProject,
        )
        !sessionsVisible -> ProjectToolsFrame(project.id) {
            when {
                composing -> key(project.id) {
                    NewTaskComposer(
                        lastRuntime = AgentRuntime.entries.firstOrNull { runtime -> runtime.name == liveState.lastUsedAgent },
                        onStart = { runtime -> navigation.beginLaunch("", runtime)?.let(launch) },
                        onCancel = navigation::cancelComposition,
                    )
                }
                tabContent != null -> tabContent()
                task == null -> WorkbenchWelcome(project.displayName, projectRoot, openProject) {
                    navigation.newTaskRequest = project.id
                }
                else -> TaskLaunchStatus(task.title, navigation.failedLaunches[task.id], onRetry = launch)
            }
        }
        else -> {
        checkNotNull(task)
        // This branch alone mounts the session surface. A selected row behind a placeholder is never viewed.
        val responseRows = rememberAgentSessionRows(sessions, rememberTerminalRevision(sessions).value, scans = false)
        val focused = LocalWindowInfo.current.isWindowFocused
        val responseVisible = sessionsVisible && navigation.tab == ProjectTab.Work
        ObserveViewedSessionResponse(responseRows, task.id, responseVisible, focused)
        val codeSlots = rememberWorkbenchCodeSlots(
            source = bindings.codePanelSource,
            saved = layout?.layout?.codePanels.orEmpty(),
            onSavedChange = { panels -> layout?.update { saved -> saved.copy(codePanels = saved.codePanels + panels) } },
        )
        // Each terminal's header names its branch from the same reads the Code panel follows worktrees with.
        val sessionBranches = remember(task.id) { SessionBranches() }
        val branchOf: SessionBranchReader? = bindings.codePanelSource?.let { source ->
            { sessionId, folder -> sessionBranches.branch(sessionId, source.worktrees(folder), source.sessionWorktreeRoots(sessionId)) }
        }
        WorkbenchScreen(
            workspacePath = viewFolder,
            activeTerminalKey = task.id,
            activeTerminalName = task.title,
            activeTaskState = task.displayState,
            publishesSelection = publishesSelection,
            taskAgent = AgentRuntime.entries.firstOrNull { runtime -> runtime.name == task.lastAgent },
            terminalSessionManager = sessions,
            terminalWorkspaceRepository = terminalWorkspaceRepository,
            paneSlots = bindings.paneSlots.copy(
                terminal = bindings.paneSlots.terminal.copy(branchOf = branchOf), code = codeSlots,
            ),
            onStartSession = { runtime ->
                startAdditionalTaskSession(
                    sessions, task.id, viewFolder,
                    settings.terminal.shellCommand?.takeIf(String::isNotBlank) ?: "zsh", runtime,
                ) { session -> bindings.startTerminal(session, settings, colors) }.exceptionOrNull()?.let {
                    strings.couldNotStart(runtime.displayName, it.message ?: strings.unknownError)
                }
            },
            defaultShell = settings.terminal.shellCommand?.takeIf(String::isNotBlank) ?: "zsh",
            terminalFontFamily = settings.effectiveTerminalFontFamily,
            terminalFontSize = settings.effectiveTerminalFontSize,
            terminalScrollbackLines = settings.effectiveScrollbackLines,
            onOpenExternalTerminal = onOpenExternalTerminal,
            newSessionRequested = navigation.newSessionRequest == task.id,
            onNewSessionRequestHandled = { navigation.newSessionRequest = null },
            onTerminalActivated = { activatedTask, sessionId ->
                acknowledgeViewedSessionResponse(responseRows, sessionId, activatedTask, responseVisible, focused)
                onTerminalActivated(activatedTask, sessionId)
            },
            besideNewSessionRequest = navigation.newSessionRequest,
            // A session started from the Task beside goes into that Task, in the folder its terminals work in.
            onStartBesideSession = { besideId, runtime ->
                startAdditionalTaskSession(
                    sessions, besideId, navigation.executionFolder(besideId, sessions) ?: projectRoot,
                    settings.terminal.shellCommand?.takeIf(String::isNotBlank) ?: "zsh", runtime,
                ) { session -> bindings.startTerminal(session, settings, colors) }.exceptionOrNull()?.let {
                    strings.couldNotStart(runtime.displayName, it.message ?: strings.unknownError)
                }
            },
            resultPanes = layout?.layout?.resultPanes.orEmpty(),
            onResultPanesChange = { panes -> layout?.update { saved -> saved.copy(resultPanes = saved.resultPanes + panes) } },
            // A Project window coming back hands its Tasks' Result panes to this window, as it left them.
            resultPanesEpoch = projectWindows?.windows?.map { it.projectId },
            resultShare = layout?.layout?.resultShare,
            onResultShareChange = { share -> layout?.update { saved -> saved.copy(resultShare = share) } },
            // Tasks beside are Experimental; while off they stay saved but are not drawn. One Task of this Project at most.
            besideTasks = listOfNotNull(
                BesideTaskIds.shown(layout?.layout?.besideTaskIds.orEmpty(), task.id, liveState.projects)
                    .takeIf { settings.experimental.tasksBeside },
            ).mapNotNull { id ->
                // A Task deleted or in a closed Project is skipped until it comes back.
                // The live rows carry the state the sidebar shows, so the header says the same.
                val owner = liveState.projects.firstOrNull { project -> project.tasks.any { it.id == id } }
                val row = owner?.tasks?.firstOrNull { it.id == id } ?: return@mapNotNull null
                val folder = navigation.executionFolder(id, sessions) ?: owner.rootPath ?: return@mapNotNull null
                BesideTask(id = id, title = row.title, folder = folder, state = row.displayState)
            },
            besideSplit = layout?.layout?.besideSplit,
            onBesideSplitChange = { split -> layout?.update { saved -> saved.copy(besideSplit = split) } },
            onCloseBeside = { id -> layout?.update { saved -> saved.copy(besideTaskIds = saved.besideTaskIds - id) } },
            onSelectBeside = { id ->
                val projectId = liveState.projects.firstOrNull { project -> project.tasks.any { it.id == id } }?.id
                if (projectId != null) {
                    // The Task left behind takes the chosen one's place beside, so neither drops out of view.
                    layout?.update { saved ->
                        saved.copy(besideTaskIds = saved.besideTaskIds.map { beside -> if (beside == id) task.id else beside })
                    }
                    navigation.selectTask(projectId, id)
                }
            },
            besideTerminalSplits = layout?.layout?.terminalSplits.orEmpty(),
            onBesideTerminalSplitChange = { id, split ->
                layout?.update { saved -> saved.copy(terminalSplits = saved.terminalSplits + (id to split)) }
            },
            terminalSplit = layout?.layout?.terminalSplits?.get(task.id),
            onTerminalSplitChange = { split ->
                layout?.update { saved -> saved.copy(terminalSplits = saved.terminalSplits + (task.id to split)) }
            },
            content = tabContent?.let { drawn -> { modifier -> Box(modifier) { drawn() } } },
        )
        }
    }
}
