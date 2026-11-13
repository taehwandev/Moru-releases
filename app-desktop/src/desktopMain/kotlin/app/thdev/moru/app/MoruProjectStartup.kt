package app.thdev.moru.app

import app.thdev.moru.core.data.defaultDesktopWorkbenchLayoutRepository
import app.thdev.moru.core.data.defaultDesktopProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.domain.ProjectCatalogSnapshot
import app.thdev.moru.core.domain.ProjectEntry
import java.io.File

/** Launch-time folder selection precedes project-scoped storage and visible terminals. */
fun createMoruStartupDependencies(
    arguments: Array<String>,
    configuredPath: String? = System.getProperty("moru.projectRoot")
        ?: System.getenv("MORU_PROJECT_ROOT"),
    userDir: File = File(System.getProperty("user.dir")),
    userHome: File = File(System.getProperty("user.home")),
    appDataDir: File = resolveMoruAppDataDir(),
    projectCatalogRepository: ProjectCatalogRepository = defaultDesktopProjectCatalogRepository(appDataDir),
): MoruDesktopDependencies {
    val startup = resolveMoruProjectStartup(
        launch = resolveMoruProjectLaunch(arguments, configuredPath, userDir, userHome),
        repository = projectCatalogRepository,
        storageRoot = defaultMoruStorageRoot(userHome),
    )
    return createMoruDesktopDependencies(
        projectRoot = startup.root,
        projectCatalogRepository = projectCatalogRepository,
        startupNotice = startup.notice,
        registerProject = false,
        layoutRepository = defaultDesktopWorkbenchLayoutRepository(appDataDir),
    )
}

internal data class MoruProjectStartup(val root: File, val notice: String? = null)

internal fun resolveMoruProjectStartup(
    launch: MoruProjectLaunch,
    repository: ProjectCatalogRepository,
    storageRoot: File,
): MoruProjectStartup {
    if (launch.notice != null) {
        repository.updateSelection(null, null)
        return MoruProjectStartup(storageRoot, launch.notice)
    }

    val catalog = repository.loadCatalog()
    if (!launch.explicit) {
        val restored = catalog.projects.firstOrNull { it.project.id == catalog.selectedProjectId }
            ?.takeIf { it.reachableRoot() != null }
            ?: catalog.projects.firstOrNull { it.reachableRoot() != null }
        if (restored != null) {
            selectCatalogProject(repository, catalog, restored)
            return MoruProjectStartup(requireNotNull(restored.reachableRoot()))
        }
    }

    if (launch.root != null) {
        val registration = repository.registerProject("", launch.root.path)
        val registered = repository.loadCatalog().projects.single { it.project.id == registration.project.id }
        selectCatalogProject(repository, catalog, registered)
        return MoruProjectStartup(launch.root)
    }

    repository.updateSelection(null, null)
    val missingSelection = catalog.projects.any { it.project.id == catalog.selectedProjectId }
    return MoruProjectStartup(
        root = storageRoot,
        notice = if (missingSelection) {
            "The last project folder is unavailable. Open a folder to continue; your Tasks are still saved."
        } else null,
    )
}

internal fun registerAndSelectProject(repository: ProjectCatalogRepository, root: File) {
    val catalog = repository.loadCatalog()
    val registration = repository.registerProject("", root.path)
    val registered = repository.loadCatalog().projects.single { it.project.id == registration.project.id }
    selectCatalogProject(repository, catalog, registered)
}

private fun selectCatalogProject(
    repository: ProjectCatalogRepository,
    catalog: ProjectCatalogSnapshot,
    entry: ProjectEntry,
) {
    val selectedTask = catalog.selectedTaskId?.takeIf { id -> entry.tasks.any { it.id == id } }
        ?: entry.project.lastSelectedTaskId?.takeIf { id -> entry.tasks.any { it.id == id } }
    repository.updateSelection(entry.project.id, selectedTask)
}

private fun ProjectEntry.reachableRoot(): File? = project.primaryRoot
    ?.let { File(it.canonicalPath ?: it.lastKnownPath).readableCanonicalDirectory() }
