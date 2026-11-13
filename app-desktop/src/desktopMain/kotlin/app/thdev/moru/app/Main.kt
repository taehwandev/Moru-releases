package app.thdev.moru.app

import app.thdev.moru.core.model.AppIcon
import androidx.compose.ui.Modifier
import app.thdev.moru.core.model.effectiveScrollbackLines
import app.thdev.moru.core.model.effectiveTerminalFontFamily
import app.thdev.moru.core.model.effectiveTerminalFontSize
import app.thdev.moru.core.terminal.host.TerminalHostLauncher
import app.thdev.moru.core.terminal.host.TerminalHostMain
import app.thdev.moru.core.terminal.external.openExternalTerminalForSession
import app.thdev.moru.core.terminal.iterm.ITerm2ProfileImporter
import app.thdev.moru.core.system.DesktopAttentionNotifier
import app.thdev.moru.core.data.AgentWorkflowSourceScanner
import app.thdev.moru.core.data.git.DesktopGitRepository
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.material.prepareMoruNativeWindow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.thdev.moru.feature.terminal.startDesktopTerminalSession
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import app.thdev.moru.core.terminal.TerminalSessionKind
import java.awt.Desktop
import java.awt.EventQueue
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // The packaged app has no separate `java`, so the terminal host is this same program started with a flag.
    if (args.firstOrNull() == TerminalHostLauncher.HostArgument) {
        TerminalHostMain.main(args.drop(1).toTypedArray())
        return
    }
    val crashes = MoruCrashReports(resolveMoruAppDataDir())
    MoruErrorLog.install(resolveMoruAppDataDir(), onUncaught = crashes::crashed)
    TerminalHostLauncher.configure(resolveMoruAppDataDir())
    ResponseAcknowledgments(resolveMoruAppDataDir()).restore()
    MoruDesktopRuntimeProperties.configure()
    MoruDesktopRuntimeProperties.warmUpGraphicsEnvironmentOnFirstThread()
    crashes.offerPrevious()

    // A failure before the window opens would otherwise end Moru without a word; it says why, then quits.
    val desktopDependencies = runCatching { createMoruStartupDependencies(args) }.getOrElse { error ->
        showStartupFailure(error)
        exitProcess(1)
    }
    runComposeApplication(desktopDependencies)
}

@OptIn(FlowPreview::class, ExperimentalComposeUiApi::class)
private fun runComposeApplication(desktopDependencies: MoruDesktopDependencies) {
    application {
        KeepResponseAcknowledgments(remember { ResponseAcknowledgments(resolveMoruAppDataDir()) })
        DisposableEffect(Unit) {
            val popupPolicy = MoruPopupWindowPolicy.install()
            onDispose { popupPolicy.close() }
        }
        val dependencies = remember { desktopDependencies }
        DisposableEffect(dependencies) {
            onDispose { dependencies.closeNotifications() }
        }
        val commands = remember { MoruCommands() }
        // The icon chosen in Settings › Appearance, in the Dock and on the window, from the saved settings.
        val iconChoice = AppIcon.fromId(dependencies.appState.settings.appearance.appIcon)
        val applicationIcon = remember(iconChoice) { MoruDesktopRuntimeProperties.applicationIcon(iconChoice) }
        LaunchedEffect(applicationIcon) { MoruDockIcon.show(applicationIcon) }
        val settingsRepository = dependencies.settingsRepository
        val initialSettings = dependencies.appState.settings
        val windowState = rememberWindowState(
            placement = MoruWindowStatePolicy.initialPlacement(
                initialSettings.window,
                initialSettings.general.rememberLastState,
            ),
            position = MoruWindowStatePolicy.initialPosition(
                initialSettings.window,
                initialSettings.general.rememberLastState,
            ),
            size = MoruWindowStatePolicy.initialSize(
                initialSettings.window,
                initialSettings.general.rememberLastState,
            ),
        )
        // While set, the quit question is open for this many running terminals.
        var askingQuit by remember { mutableStateOf<Int?>(null) }
        // One layout keeper and one set of Lab sessions for every window, so their saves never overwrite each other.
        val layout = remember(dependencies) { WorkbenchLayoutKeeper(dependencies.layoutRepository) }
        val checkpointTerminals = rememberTerminalConversationCheckpoint(dependencies.terminalSessionManager, layout)
        // A layout change made a moment before quit is still waiting for its save; it is written before the app exits.
        val finishQuit = rememberTerminalQuit(
            beforeTeardown = checkpointTerminals,
            beforeExit = layout::flush,
        ) { exitApplication() }
        // Closing the window, Command+Q and the app menu's Quit all come here; Remember Last State decides the terminals.
        val requestQuit: () -> Unit = {
            MoruWindowStatePolicy.persist(settingsRepository, windowState)
            val running = dependencies.terminalSessionManager.listSessions().count { it.spec.kind == TerminalSessionKind.TERMINAL }
            when (quitPlan(dependencies.appState.settings.general.rememberLastState, running)) {
                // Only lets go: the terminals keep running in the terminal host and come back on the next start.
                QuitPlan.Detach -> finishQuit(true)
                QuitPlan.EndAll -> finishQuit(false)
                QuitPlan.Ask -> askingQuit = running
            }
        }
        DisposableEffect(Unit) {
            val desktop = Desktop.getDesktop().takeIf { Desktop.isDesktopSupported() && it.isSupported(Desktop.Action.APP_QUIT_HANDLER) }
            desktop?.setQuitHandler { _, response ->
                response.cancelQuit()
                EventQueue.invokeLater { requestQuit() }
            }
            onDispose { desktop?.setQuitHandler(null) }
        }
        askingQuit?.let { running ->
            QuitConfirmDialog(
                dependencies, running,
                onQuit = { keep ->
                    askingQuit = null
                    finishQuit(keep)
                },
                onCancel = { askingQuit = null },
            )
        }
        KeepAppQuitCommands(commands, persistWindow = { MoruWindowStatePolicy.persist(settingsRepository, windowState) }, finishQuit)

        val projectWindows = remember(layout) { ProjectWindows(layout, dependencies.projectCatalogRepository) }
        SwingWindow(
            onCloseRequest = requestQuit,
            state = windowState,
            init = ::prepareMoruNativeWindow,
            title = MoruDesktopRuntimeProperties.applicationName,
            icon = remember(applicationIcon) { BitmapPainter(applicationIcon.toComposeImageBitmap()) },
        ) {
            // The window's size and place are kept as they change, not only on a normal close, so a crash or a kill keeps them.
            LaunchedEffect(windowState) {
                snapshotFlow { Triple(windowState.size, windowState.position, windowState.placement) }
                    .drop(1)
                    .debounce(WindowSaveDelayMillis)
                    .collect { withContext(Dispatchers.IO) { MoruWindowStatePolicy.persist(settingsRepository, windowState) } }
            }
            LaunchedEffect(Unit) {
                MoruWindowChrome.apply(window.rootPane)
                EventQueue.invokeLater {
                    window.toFront()
                    window.requestFocus()
                }
            }
            MoruDesktopPopupHost(
                window,
                darkTheme = !dependencies.appState.settings.appearance.theme
                    .resolveColors(androidx.compose.foundation.isSystemInDarkTheme()).isLight,
                materialStyle = if (dependencies.appState.settings.appearance.appleStyle)
                    app.thdev.moru.core.designsystem.theme.MoruMaterialStyle.Apple
                else app.thdev.moru.core.designsystem.theme.MoruMaterialStyle.Standard,
            ) {
                MoruApplicationAppearance(dependencies)
                MoruMenuBar(dependencies, commands)
                MoruWindowContent(dependencies, commands, layout, projectWindows = projectWindows)
            }
        }
        OpenProjectWindows(dependencies, layout, projectWindows, commands)
    }
}

/** A window's workbench: every Project in the main window, or the one Project of a Project window. */
@Composable
internal fun MoruWindowContent(
    dependencies: MoruDesktopDependencies,
    commands: MoruCommands,
    layout: WorkbenchLayoutKeeper,
    windowProjectId: String? = null,
    projectWindows: ProjectWindows? = null,
) {
    // The active workbench root follows the selected Project; it starts at the
    // launch root and changes when a Project is picked in the sidebar.
    var activeRoot by remember(dependencies.projectRoot) { mutableStateOf(dependencies.projectRoot) }
    var hasProject by remember(dependencies) { mutableStateOf(dependencies.initiallyHasProject) }
    // Files, tree and git status follow the folder being viewed: the folder the selected Task's terminal runs in, else the project.
    var viewRoot by remember(dependencies.projectRoot) { mutableStateOf(dependencies.projectRoot) }
    var gitStatusRefreshKey by remember(dependencies.projectRoot) { mutableStateOf(0) }
    var treeRefreshKey by remember(dependencies.projectRoot) { mutableStateOf(0) }
    val displayedRoot = viewRoot.absoluteFile
    val folderData = rememberWorkbenchFolderData(
        displayedRoot,
        hasProject,
        treeRefreshKey,
        gitStatusRefreshKey,
        scanGit = { folder -> dependencies.gitReader.status(folder.absolutePath) },
        scanTree = { folder -> dependencies.projectFiles.tree(folder.absolutePath) },
    )
    val codePanelSource = remember(dependencies) { DesktopCodePanelSource(dependencies.gitReader, dependencies.projectFiles) }
    ReportResultChanges(codePanelSource)
    val gitRepository = remember { DesktopGitRepository() }
    val attentionNotifier = remember { DesktopAttentionNotifier() }
    MoruApp(
        workspacePath = activeRoot.absolutePath,
        terminalSessionManager = dependencies.terminalSessionManager,
        runtimeStatusRepository = dependencies.runtimeStatusRepository,
        settingsRepository = dependencies.settingsRepository,
        terminalWorkspaceRepository = dependencies.terminalWorkspaceRepository,
        projectCatalogRepository = dependencies.projectCatalogRepository,
        startupNotice = dependencies.startupNotice,
        appState = dependencies.appState,
        gitRepository = gitRepository,
        systemLanguageTag = java.util.Locale.getDefault().toLanguageTag(),
        commands = commands,
        windowTopInset = MoruWindowChrome.topInsetDp().dp,
        attentionNotifier = attentionNotifier,
        notificationRepository = dependencies.notificationRepository,
        scanWorkflowSources = remember { AgentWorkflowSourceScanner() }::scan,
        loadKnowledgeDocuments = remember { app.thdev.moru.core.data.knowledge.DesktopKnowledgeRepository() }::documents,
        openKnowledgeDocument = remember { app.thdev.moru.core.data.knowledge.DesktopKnowledgeRepository() }::open,
        openKnowledgeLink = remember { app.thdev.moru.core.data.knowledge.DesktopKnowledgeRepository() }::openLink,
        onPickFolder = { title, startIn -> pickProjectFolder(startIn = startIn?.let { java.io.File(it) }, title = title) },
        firstLaunch = dependencies.firstLaunch,
        layout = layout,
        windowProjectId = windowProjectId,
        projectWindows = projectWindows,
        onPickProjectFolder = { pickProjectFolder(startIn = activeRoot.takeIf { hasProject }?.parentFile) },
        onSelectProjectRoot = { rootPath -> activeRoot = java.io.File(rootPath); hasProject = true },
        onViewFolderChange = { folder -> viewRoot = java.io.File(folder) },
        projectDataRoot = displayedRoot.absolutePath,
        projectTreeEntries = folderData.treeEntries,
        gitStatus = folderData.gitStatus,
        onRefreshGitStatus = { gitStatusRefreshKey += 1 },
        loadGitHistory = dependencies.gitReader::history,
        loadCommitDiff = dependencies.gitReader::commitChanges,
        loadWorkingChanges = dependencies.gitReader::workingChanges,
        codePanelSource = codePanelSource,
        onOpenProjectFile = { path ->
            dependencies.projectFiles.open(displayedRoot.absolutePath, path)
        },
        onSaveProjectFile = { path, content ->
            dependencies.projectFiles.save(displayedRoot.absolutePath, path, content)
        },
        onCreateProjectFile = { path ->
            dependencies.projectFiles.create(displayedRoot.absolutePath, path)
        },
        onRenameProjectFile = { fromPath, toPath ->
            dependencies.projectFiles.rename(displayedRoot.absolutePath, fromPath, toPath)
        },
        onDeleteProjectFile = { path ->
            dependencies.projectFiles.delete(displayedRoot.absolutePath, path)
        },
        onRefreshProjectTree = { treeRefreshKey += 1 },
        onLoadProjectFolder = { path -> dependencies.projectFiles.children(displayedRoot.absolutePath, path) },
        heldTerminalKeys = TerminalHostLauncher::heldKeys,
        terminateHeldTerminal = { key ->
            kotlin.concurrent.thread(isDaemon = true, name = "moru-host-terminate") { TerminalHostLauncher.terminate(key) }
        },
        onStartTerminalSession = { session, settings, colors ->
            val manager = dependencies.terminalSessionManager
            startDesktopTerminalSession(
                sessionKey = session.id,
                workingDirectory = session.spec.workingDirectory,
                shell = session.spec.shell,
                startupCommand = session.spec.startupCommand,
                onCommandCaptured = { manager.recordCommandHistory(session.id, it) },
                fontFamily = settings.effectiveTerminalFontFamily,
                fontSize = settings.effectiveTerminalFontSize,
                scrollbackLines = settings.effectiveScrollbackLines,
                onOutput = { manager.reportOutput(session.id, it) },
                onExit = { manager.reportExit(session.id, it) },
                colors = colors,
                hostKey = session.spec.hostKey,
                profile = settings.terminal.profile,
                optionSendsEscape = settings.terminal.optionSendsEscape,
            )
        },
        onOpenExternalTerminal = { session ->
            val result = openExternalTerminalForSession(session)
            if (!result.opened) {
                System.err.println("External terminal fallback failed: ${result.message}")
            }
        },
        importTerminalProfile = { withContext(Dispatchers.IO) { ITerm2ProfileImporter.import() } },
    )
}

internal const val WindowSaveDelayMillis = 500L

/** The app's quit-and-end command, which quits the way closing the main window does after [persistWindow]. */
@Composable
private fun KeepAppQuitCommands(
    commands: MoruCommands,
    persistWindow: () -> Unit,
    finishQuit: (keepTerminals: Boolean) -> Unit,
) {
    DisposableEffect(commands) {
        commands.quitEndingTerminals = {
            persistWindow()
            finishQuit(false)
        }
        onDispose { commands.quitEndingTerminals = null }
    }
}
