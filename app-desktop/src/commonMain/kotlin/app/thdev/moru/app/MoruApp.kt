package app.thdev.moru.app

import app.thdev.moru.feature.settings.onboarding.MoruOnboarding
import androidx.compose.foundation.layout.padding
import app.thdev.moru.core.model.AppIcon
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.app.i18n.appStrings
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.data.InMemoryNotificationRepository
import app.thdev.moru.core.designsystem.theme.*
import app.thdev.moru.core.model.AgentWorkflowSettings
import app.thdev.moru.core.model.AgentWorkflowSource
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.terminal.TerminalProfileImport
import app.thdev.moru.feature.terminal.LocalTerminalOptionSendsEscape
import app.thdev.moru.feature.terminal.LocalTerminalProfile
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.system.AttentionNotifier
import app.thdev.moru.core.system.NoAttentionNotifier
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSnapshot
import app.thdev.moru.core.runtime.RuntimeStatusRepository
import app.thdev.moru.core.ui.path.LocalHomeDirectory
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import app.thdev.moru.feature.workbench.resume.RunCliUpdateRestarts
import app.thdev.moru.feature.settings.SettingsScreen
import app.thdev.moru.feature.settings.rememberSettingsEditorState
import app.thdev.moru.feature.workbench.*
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.feature.codepanel.CodePanelSource
import app.thdev.moru.feature.workbench.slots.WorkbenchPaneSlots
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Composable
fun MoruApp(
    workspacePath: String,
    terminalSessionManager: TerminalSessionManager,
    runtimeStatusRepository: RuntimeStatusRepository,
    settingsRepository: SettingsRepository,
    terminalWorkspaceRepository: TerminalWorkspaceRepository,
    projectCatalogRepository: ProjectCatalogRepository,
    startupNotice: String? = null,
    onPickProjectFolder: () -> String? = { null },
    onSelectProjectRoot: (String) -> Unit = {},
    onViewFolderChange: (String) -> Unit = {},
    projectDataRoot: String? = null,
    projectTreeEntries: List<WorkbenchProjectTreeEntry> = emptyList(),
    gitStatus: WorkbenchGitStatus = WorkbenchGitStatus.NotRepository,
    onRefreshGitStatus: () -> Unit = {},
    loadGitHistory: (workspacePath: String) -> GitHistoryResult = {
        GitHistoryResult.Unavailable("Git history is unavailable.")
    },
    loadCommitDiff: (workspacePath: String, hash: String) -> CommitDiffResult = { _, _ ->
        CommitDiffResult.Unavailable("Commit diff is unavailable.")
    },
    loadWorkingChanges: (workspacePath: String) -> CommitDiffResult = {
        CommitDiffResult.Unavailable("Working changes are unavailable.")
    },
    onOpenProjectFile: (String) -> WorkbenchFilePreview = { path ->
        WorkbenchFilePreview.unavailable(path, "File preview is not wired.")
    },
    onSaveProjectFile: (String, String) -> WorkbenchFileSaveOutcome = { path, _ ->
        WorkbenchFileSaveOutcome.Failed(path, "Saving is not wired.")
    },
    onCreateProjectFile: (String) -> WorkbenchFileCreateOutcome = { path ->
        WorkbenchFileCreateOutcome.Failed(path, "Creating files is not wired.")
    },
    onRenameProjectFile: (String, String) -> WorkbenchFileRenameOutcome = { path, _ ->
        WorkbenchFileRenameOutcome.Failed(path, "Renaming files is not wired.")
    },
    onDeleteProjectFile: (String) -> WorkbenchFileDeleteOutcome = { path ->
        WorkbenchFileDeleteOutcome.Failed(path, "Deleting files is not wired.")
    },
    onRefreshProjectTree: () -> Unit = {},
    onLoadProjectFolder: (String) -> List<WorkbenchProjectTreeEntry> = { emptyList() },
    onOpenExternalTerminal: (TerminalSessionSnapshot) -> Unit = {},
    onStartTerminalSession: (TerminalSessionSnapshot, AppSettings, MoruColors) -> Unit = { _, _, _ -> },
    heldTerminalKeys: () -> Set<String>? = { null },
    terminateHeldTerminal: (hostKey: String) -> Unit = {},
    appState: MoruAppState? = null,
    systemLanguageTag: String = "en",
    commands: MoruCommands = MoruCommands(),
    windowTopInset: Dp = 0.dp,
    gitRepository: GitRepository? = null,
    /** Reads the code each Task terminal changes; null leaves Tasks without a Code panel. */
    codePanelSource: CodePanelSource? = null,
    /** The terminal and preview views the workbench draws; tests may pass stand-ins. */
    paneSlots: WorkbenchPaneSlots = DesktopWorkbenchPaneSlots,
    attentionNotifier: AttentionNotifier = NoAttentionNotifier,
    layout: WorkbenchLayoutKeeper? = null,
    notificationRepository: NotificationRepository? = null,
    scanWorkflowSources: (AgentWorkflowSettings, String) -> List<AgentWorkflowSource> = { _, _ -> emptyList() },
    loadKnowledgeDocuments: (String) -> app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex = {
        app.thdev.moru.core.model.knowledge.KnowledgeDocumentIndex(error = "Document discovery is unavailable.")
    },
    openKnowledgeDocument: (String, String) -> WorkbenchFilePreview = { _, path -> WorkbenchFilePreview.unavailable(path, "Document reading is unavailable.") },
    openKnowledgeLink: (String, String, String) -> WorkbenchFilePreview = { _, _, target -> WorkbenchFilePreview.unavailable(target, "Document links are unavailable.") },
    onPickFolder: (title: String, startIn: String?) -> String? = { _, _ -> null },
    /** Moru's own update line, shown above every destination; the real app passes its update banner. */
    updateBanner: @Composable () -> Unit = {},
    appVersion: String? = null,
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    onRestartForUpdate: () -> Unit = {},
    /** Draws each app icon Settings offers; the real app passes the pictures, null hides the choice. */
    appIconPreview: (@Composable (AppIcon, Modifier) -> Unit)? = null,
    /** True when the welcome is due on this computer (a new user, or one who left it unfinished): the main window opens with it. */
    firstLaunch: Boolean = false,
    /** Reads another terminal app's profile for Settings; the real app reads iTerm2's. */
    importTerminalProfile: suspend () -> TerminalProfileImport = { TerminalProfileImport.NotFound },
    /** The Project this window shows on its own, or null for the main window with every Project. */
    windowProjectId: String? = null,
    /** The Projects open in windows of their own (Experimental); null when there are none to keep. */
    projectWindows: ProjectWindows? = null,
) {
    var currentScreen by remember { mutableStateOf(MoruDestination.Workbench) }
    val appScope = rememberCoroutineScope()
    val inbox = rememberNotificationInbox(notificationRepository, appScope)
    val state = remember(settingsRepository, appState) { appState ?: MoruAppState(settingsRepository) }
    val settingsEditor = rememberSettingsEditorState(settingsRepository, state::settingsSaved)
    val settings = state.settings
    val uiLanguage = resolveUiLanguage(settings.appearance.language, systemLanguageTag)
    val currentLanguage = rememberUpdatedState(uiLanguage)
    // Every window announces its catalog changes; a Project window sees its Project only, with its own selection.
    val announcing = remember(projectCatalogRepository) { AnnouncingCatalog(projectCatalogRepository) }
    val catalog = remember(announcing, windowProjectId) {
        windowProjectId?.let { ProjectWindowCatalog(announcing, it) } ?: announcing
    }
    val navigation = remember(catalog) {
        WorkbenchNavigation(
            catalog, startupNotice.takeIf { windowProjectId == null },
            initiallyCollapsed = layout?.layout?.collapsedProjectIds?.toSet().orEmpty(),
            initialGeneral = settings.general,
        ) { appStrings(currentLanguage.value) }
    }
    LaunchedEffect(settings.general) { navigation.applyGeneralSettings(settings.general) }
    val catalogRevision = CatalogChanges.revision
    // A window has read its own changes already, so it reads the catalog again only for another window's.
    LaunchedEffect(catalogRevision) { if (announcing.othersChanged(catalogRevision)) navigation.reload() }
    val mainWindow = windowProjectId == null
    val sidebarOrderChanges = remember(navigation, settingsEditor) { SidebarOrderChanges(navigation, settingsEditor) }
    KeepAppCommands(commands, navigation, settings.keyboard) { currentScreen = it }
    // The welcome opens once, in the main window, for a new user; Settings › General opens it again.
    var showOnboarding by remember { mutableStateOf(mainWindow && firstLaunch && !settings.general.onboardingDone) }
    // Work done once for the whole app runs in the main window only.
    if (mainWindow) RunCliUpdateRestarts(terminalSessionManager, paneSlots.terminal.submitLine, paneSlots.sessions.activityOf)
    val bindings = MoruWorkbenchBindings(
        onPickProjectFolder, onSelectProjectRoot, onViewFolderChange, projectTreeEntries, gitStatus,
        onRefreshGitStatus, loadGitHistory, loadCommitDiff, loadWorkingChanges,
        onOpenProjectFile, onSaveProjectFile, onCreateProjectFile,
        onRenameProjectFile, onDeleteProjectFile, onRefreshProjectTree, projectDataRoot, onStartTerminalSession,
        gitRepository, heldTerminalKeys, codePanelSource, paneSlots, onLoadProjectFolder,
        terminateHeldTerminal = terminateHeldTerminal,
        pickFolder = onPickFolder,
    )
    MoruTheme(
        materialStyle = if (settings.appearance.appleStyle) app.thdev.moru.core.designsystem.theme.MoruMaterialStyle.Apple
            else app.thdev.moru.core.designsystem.theme.MoruMaterialStyle.Standard,
        colors = settings.appearance.theme.resolveColors(isSystemInDarkTheme()),
        typography = app.thdev.moru.core.designsystem.theme.appTypography(
            settings.appearance.uiFontFamily, settings.appearance.uiFontSize,
        ),
    ) {
        CompositionLocalProvider(
            LocalUiLanguage provides uiLanguage,
            LocalHomeDirectory provides System.getProperty("user.home").orEmpty(),
            LocalTerminalProfile provides settings.terminal.profile,
            LocalTerminalOptionSendsEscape provides settings.terminal.optionSendsEscape,
        ) {
            val colors = MoruTheme.colors
            val viewedTaskId = navigation.sidebar.selectedTaskId.takeIf { currentScreen == MoruDestination.Workbench }
            if (mainWindow) MoruAttentionHost(navigation, terminalSessionManager, attentionNotifier,
                settings, inbox::record, viewedTaskId,
                unreadTaskIds = inbox.unreadTaskIds, onTasksRead = inbox::markTasksRead) {
                currentScreen = MoruDestination.Workbench
            }
            if (showOnboarding) {
                MoruOnboarding(
                    editor = settingsEditor,
                    onFinish = {
                        settingsEditor.change { s -> s.copy(general = s.general.copy(onboardingDone = true)) }
                        showOnboarding = false
                        currentScreen = MoruDestination.Workbench
                    },
                    onOpenProject = { onPickProjectFolder()?.let(navigation::openProject) },
                    modifier = Modifier.padding(top = windowTopInset),
                )
                return@CompositionLocalProvider
            }
            MoruFrame(currentScreen, onSelect = { currentScreen = it }, topInset = windowTopInset,
                unreadNotificationCount = inbox.unreadTaskIds.size,
                inbox = { close ->
                    NotificationInboxRoute(
                        inbox = inbox,
                        navigation = navigation,
                        appVersion = appVersion,
                    ) {
                        close()
                        currentScreen = MoruDestination.Workbench
                    }
                },
                banner = updateBanner,
                appVersion = appVersion,
                updateStatus = updateStatus,
                onCheckForUpdates = commands.checkForUpdates,
                onRestartForUpdate = onRestartForUpdate,
                toolbar = { modifier ->
                    if (currentScreen == MoruDestination.Workbench) {
                        MoruProjectToolbar(navigation, bindings, terminalSessionManager, modifier = modifier)
                    } else androidx.compose.foundation.layout.Spacer(modifier)
                }) {
                when (currentScreen) {
                    // The frame opens the inbox as a panel over the current screen; it is never a screen of its own.
                    MoruDestination.Notifications -> Unit
                    MoruDestination.Knowledge -> KnowledgeSourcesScreen(
                        workspacePath = navigation.selectedProject?.rootPath ?: workspacePath,
                        workflows = settings.workflows,
                        scanWorkflowSources = scanWorkflowSources,
                        onWorkflowsChange = { workflows ->
                            state.settingsSaved(settingsRepository.updateSettings { it.copy(workflows = workflows) })
                        },
                        onPickFolder = onPickFolder,
                        loadDocuments = loadKnowledgeDocuments,
                        openDocument = openKnowledgeDocument,
                        openDocumentLink = openKnowledgeLink,
                    )
                    MoruDestination.Workbench -> MoruWorkbenchHost(
                        navigation, bindings, appScope, workspacePath, terminalSessionManager,
                        terminalWorkspaceRepository, settings, onOpenExternalTerminal, commands,
                        showWorkbench = { currentScreen = MoruDestination.Workbench },
                        layout = layout,
                        onReorderProjects = sidebarOrderChanges::projects,
                        onReorderTasks = sidebarOrderChanges::tasks,
                        orderingFeedback = {
                            if (settingsEditor.saveStatus == app.thdev.moru.feature.settings.SettingsSaveStatus.Failed) {
                                app.thdev.moru.feature.settings.SettingsSaveFeedback(
                                    settingsEditor.saveStatus, settingsEditor.saveError, false, settingsEditor::retry,
                                )
                            }
                        },
                        projectWindows = projectWindows,
                        mainWindow = mainWindow,
                    )
                    MoruDestination.Settings -> SettingsScreen(
                        runtimeStatusRepository, settingsRepository,
                        onSettingsSaved = state::settingsSaved,
                        editor = settingsEditor,
                        updateState = settingsUpdateState(appStrings(), appVersion, updateStatus, commands.checkForUpdates != null),
                        onCheckForUpdates = { commands.checkForUpdates?.invoke() },
                        onRestartForUpdate = onRestartForUpdate,
                            appIconPreview = appIconPreview,
                        onShowOnboarding = { showOnboarding = true }.takeIf { mainWindow },
                        page = layout?.layout?.settingsPage,
                        onPageChange = { page -> layout?.update { it.copy(settingsPage = page) } },
                        onRecordingShortcut = { commands.recordingShortcut = it },
                        importTerminalProfile = importTerminalProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberNotificationInbox(repository: NotificationRepository?, scope: CoroutineScope): NotificationInboxController {
    val source = remember(repository) { repository ?: InMemoryNotificationRepository() }
    return remember(source, scope) { NotificationInboxController(source, scope) }
}

