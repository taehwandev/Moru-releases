package app.thdev.moru.app

import app.thdev.moru.core.data.RoomTerminalWorkspaceRepository
import app.thdev.moru.core.data.defaultDesktopProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruDatabase
import app.thdev.moru.core.data.files.DesktopProjectFileRepository
import app.thdev.moru.core.data.git.workbench.DesktopWorkbenchGitReader
import app.thdev.moru.core.data.defaultDesktopSettingsRepository
import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectFileRepository
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.domain.WorkbenchGitReader
import app.thdev.moru.core.terminal.DesktopPtyTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionManager
import app.thdev.moru.core.runtime.DesktopRuntimeStatusRepository
import app.thdev.moru.core.runtime.RuntimeStatusRepository
import app.thdev.moru.core.data.InMemoryWorkbenchLayoutRepository
import app.thdev.moru.core.data.InMemoryNotificationRepository
import app.thdev.moru.core.data.defaultDesktopNotificationRepository
import app.thdev.moru.core.domain.NotificationRepository
import app.thdev.moru.core.domain.WorkbenchLayoutRepository
import app.thdev.moru.feature.workbench.restore.restoreTerminals
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import java.io.File

data class MoruDesktopDependencies(
    val projectRoot: File,
    val terminalSessionManager: TerminalSessionManager,
    val runtimeStatusRepository: RuntimeStatusRepository,
    val settingsRepository: SettingsRepository,
    val terminalWorkspaceRepository: TerminalWorkspaceRepository,
    val projectCatalogRepository: ProjectCatalogRepository,
    val startupNotice: String? = null,
    val gitReader: WorkbenchGitReader = DesktopWorkbenchGitReader(),
    val projectFiles: ProjectFileRepository = DesktopProjectFileRepository(),
    val layoutRepository: WorkbenchLayoutRepository = InMemoryWorkbenchLayoutRepository(),
    val notificationRepository: NotificationRepository = InMemoryNotificationRepository(),
    val closeNotifications: () -> Unit = {},
    /** The welcome is due: a new user, or one who left it unfinished (see [onboardingDue]). */
    val firstLaunch: Boolean = false,
) {
    val appState = MoruAppState(settingsRepository)
    val initiallyHasProject = projectCatalogRepository.loadCatalog().selectedProjectId != null
}

data class MoruProjectRoot(val file: File)

@DependencyGraph
interface DesktopAppGraph {
    val settingsRepository: SettingsRepository
    val runtimeStatusRepository: RuntimeStatusRepository

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides projectRoot: MoruProjectRoot): DesktopAppGraph
    }

    @Provides
    fun provideSettingsRepository(projectRoot: MoruProjectRoot): SettingsRepository {
        // A development build starts from its own defaults rather than copying the installed Moru's old project settings.
        val legacyRoot = projectRoot.file.takeUnless { MoruDesktopRuntimeProperties.development }
        return defaultDesktopSettingsRepository(resolveMoruAppDataDir(), legacyProjectRoot = legacyRoot)
    }

    @Provides
    fun provideRuntimeStatusRepository(): RuntimeStatusRepository {
        return DesktopRuntimeStatusRepository()
    }
}

fun createDesktopAppGraph(projectRoot: File): DesktopAppGraph {
    return createGraphFactory<DesktopAppGraph.Factory>()
        .create(MoruProjectRoot(projectRoot))
}

fun createMoruDesktopDependencies(
    projectRoot: File,
    projectCatalogRepository: ProjectCatalogRepository = defaultDesktopProjectCatalogRepository(resolveMoruAppDataDir()),
    startupNotice: String? = null,
    registerProject: Boolean = true,
    layoutRepository: WorkbenchLayoutRepository = InMemoryWorkbenchLayoutRepository(),
): MoruDesktopDependencies {
    val resolvedRoot = if (registerProject) {
        requireNotNull(projectRoot.readableCanonicalDirectory()) {
            "Cannot open project folder '${projectRoot.path}'. Choose an existing, readable folder."
        }
    } else projectRoot
    if (registerProject) registerAndSelectProject(projectCatalogRepository, resolvedRoot)
    val graph = createDesktopAppGraph(resolvedRoot)
    // Read once the repository exists, since making it may carry settings over from an older install.
    val settingsRepository = graph.settingsRepository
    val firstLaunch = onboardingDue(resolveMoruAppDataDir().resolve("settings.json"))
    // An existing user's settings are marked as past the welcome now, before anything saves them with the key unset.
    if (!firstLaunch) runCatching {
        settingsRepository.updateSettings { s -> if (s.general.onboardingDone) s else s.copy(general = s.general.copy(onboardingDone = true)) }
    }
    val database = defaultDesktopMoruDatabase(resolvedRoot, MoruDesktopRuntimeProperties.projectDataFolder)
    val terminalWorkspaceRepository = RoomTerminalWorkspaceRepository(database)
    val terminalSessionManager = createDefaultTerminalSessionManager()
    val notificationRepository = defaultDesktopNotificationRepository(resolveMoruAppDataDir())
    // The terminals left open last time come back before the first frame, so no Task starts with an empty one.
    restoreTerminals(terminalSessionManager, layoutRepository.loadLayout())

    return MoruDesktopDependencies(
        projectRoot = resolvedRoot,
        terminalSessionManager = terminalSessionManager,
        runtimeStatusRepository = graph.runtimeStatusRepository,
        settingsRepository = settingsRepository,
        terminalWorkspaceRepository = terminalWorkspaceRepository,
        projectCatalogRepository = projectCatalogRepository,
        startupNotice = startupNotice,
        layoutRepository = layoutRepository,
        notificationRepository = notificationRepository,
        closeNotifications = notificationRepository::close,
        firstLaunch = firstLaunch,
    )
}

private fun createDefaultTerminalSessionManager(): TerminalSessionManager {
    return DesktopPtyTerminalSessionManager(
        // Tasks create session metadata; the desktop registry owns the sole PTY.
        initialSessions = emptyList(),
        // Eager launch and visible terminal acquisition share that registry entry.
        spawnProcesses = false,
    )
}
