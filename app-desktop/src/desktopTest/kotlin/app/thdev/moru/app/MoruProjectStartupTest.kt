package app.thdev.moru.app

import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.domain.ProjectCatalogRepository
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoruProjectStartupTest {
    @Test
    fun externalTerminalFolderRegistersAndSelectsVisibleProject() = withCatalog { root, repository ->
        val project = root.resolve("external project").apply { mkdirs() }

        val startup = startup(repository, root, arguments = arrayOf("--project", project.path))

        assertEquals(project.canonicalFile, startup.root)
        val catalog = repository.loadCatalog()
        assertEquals(project.canonicalPath, catalog.projects.single().project.primaryRoot?.canonicalPath)
        assertEquals(catalog.projects.single().project.id, catalog.selectedProjectId)
        assertNull(startup.notice)
    }

    @Test
    fun configuredDefaultRegistersBeforeProjectScopedStorageIsCreated() = withCatalog { root, repository ->
        val project = root.resolve("default project").apply { mkdirs() }

        val startup = startup(repository, root, configuredPath = project.path)

        assertEquals(project.canonicalFile, startup.root)
        assertEquals(1, repository.loadCatalog().projects.size)
        assertNotNull(repository.loadCatalog().selectedProjectId)
    }

    @Test
    fun reopeningCanonicalAliasDoesNotDuplicateProjectOrLoseTask() = withCatalog { root, repository ->
        val project = root.resolve("project").apply { mkdirs() }
        project.resolve("nested").mkdirs()
        startup(repository, root, arguments = arrayOf(project.path))
        val projectId = repository.loadCatalog().selectedProjectId!!
        val taskId = repository.createTask(projectId, "Keep this Task", "Preserve the current request").id
        repository.updateSelection(projectId, taskId)

        startup(repository, root, arguments = arrayOf(project.resolve("nested/..").path))

        val catalog = repository.loadCatalog()
        assertEquals(1, catalog.projects.size)
        assertEquals(projectId, catalog.selectedProjectId)
        assertEquals(taskId, catalog.selectedTaskId)
    }

    @Test
    fun noArgumentLaunchRestoresLastSelectedProjectAndTaskBeforeCwd() = withCatalog { root, repository ->
        val project = root.resolve("saved project").apply { mkdirs() }
        val projectId = repository.registerProject("Saved", project.path).project.id
        val taskId = repository.createTask(projectId, "Resume", "Resume the same work").id
        repository.updateSelection(projectId, taskId)
        val unrelatedCwd = root.resolve("unrelated").apply { mkdirs() }

        val startup = startup(repository, root, userDir = unrelatedCwd)

        assertEquals(project.canonicalFile, startup.root)
        assertEquals(1, repository.loadCatalog().projects.size)
        assertEquals(taskId, repository.loadCatalog().selectedTaskId)
    }

    @Test
    fun invalidDefaultLeavesSavedTasksButSelectsNoUnrelatedProject() = withCatalog { root, repository ->
        val project = root.resolve("saved").apply { mkdirs() }
        val projectId = repository.registerProject("Saved", project.path).project.id
        val taskId = repository.createTask(projectId, "Saved Task", "Keep it").id
        repository.updateSelection(projectId, taskId)

        val startup = startup(repository, root, configuredPath = root.resolve("missing").path, userDir = project)

        assertTrue(startup.notice.orEmpty().contains("Cannot open project folder"))
        assertEquals(defaultMoruStorageRoot(root.resolve("home")), startup.root)
        val catalog = repository.loadCatalog()
        assertEquals(1, catalog.projects.size)
        assertEquals(taskId, catalog.projects.single().tasks.single().id)
        assertNull(catalog.selectedProjectId)
        assertNull(catalog.selectedTaskId)
    }

    @Test
    fun invalidCliFolderDoesNotUseConfiguredOrSavedProject() = withCatalog { root, repository ->
        val defaultProject = root.resolve("default").apply { mkdirs() }
        val projectId = repository.registerProject("Default", defaultProject.path).project.id
        repository.updateSelection(projectId, null)

        val startup = startup(
            repository,
            root,
            arguments = arrayOf("--project", root.resolve("missing").path),
            configuredPath = defaultProject.path,
        )

        assertNotNull(startup.notice)
        assertNull(repository.loadCatalog().selectedProjectId)
        assertEquals(1, repository.loadCatalog().projects.size)
    }

    @Test
    fun aWorkingDirectoryNobodyAskedForIsNotRegisteredAsAProject() = withCatalog { root, repository ->
        val cwd = root.resolve("terminal folder").apply { mkdirs() }

        val startup = startup(repository, root, userDir = cwd)

        assertEquals(defaultMoruStorageRoot(root.resolve("home")), startup.root)
        assertTrue(repository.loadCatalog().projects.isEmpty())
        assertNull(repository.loadCatalog().selectedProjectId)
    }

    @Test
    fun emptyGuiLaunchUsesStorageWithoutRegisteringFakeWorkspace() = withCatalog { root, repository ->
        val startup = startup(repository, root)

        assertEquals(defaultMoruStorageRoot(root.resolve("home")), startup.root)
        assertTrue(repository.loadCatalog().projects.isEmpty())
        assertNull(repository.loadCatalog().selectedProjectId)
    }

    @Test
    fun unavailableSelectionCanRestoreAnotherReachableProject() = withCatalog { root, repository ->
        val valid = root.resolve("valid").apply { mkdirs() }
        val validId = repository.registerProject("Valid", valid.path).project.id
        val missing = root.resolve("removed").apply { mkdirs() }
        val missingId = repository.registerProject("Removed", missing.path).project.id
        repository.updateSelection(missingId, null)
        missing.deleteRecursively()

        val startup = startup(repository, root)

        assertEquals(valid.canonicalFile, startup.root)
        assertEquals(validId, repository.loadCatalog().selectedProjectId)
        assertEquals(2, repository.loadCatalog().projects.size)
    }

    @Test
    fun unavailableProjectWithoutReplacementKeepsMetadataAndShowsRecovery() = withCatalog { root, repository ->
        val missing = root.resolve("removed").apply { mkdirs() }
        val projectId = repository.registerProject("Removed", missing.path).project.id
        repository.updateSelection(projectId, null)
        missing.deleteRecursively()

        val startup = startup(repository, root)

        assertTrue(startup.notice.orEmpty().contains("Open a folder to continue"))
        assertEquals(1, repository.loadCatalog().projects.size)
        assertNull(repository.loadCatalog().selectedProjectId)
    }

    private fun startup(
        repository: ProjectCatalogRepository,
        root: File,
        arguments: Array<String> = emptyArray(),
        configuredPath: String? = null,
        userDir: File = File("/"),
    ): MoruProjectStartup = resolveMoruProjectStartup(
        launch = resolveMoruProjectLaunch(arguments, configuredPath, userDir, root.resolve("home")),
        repository = repository,
        storageRoot = defaultMoruStorageRoot(root.resolve("home")),
    )

    private fun withCatalog(block: (File, ProjectCatalogRepository) -> Unit) {
        val root = Files.createTempDirectory("moru-project-startup").toFile()
        val database = defaultDesktopMoruCatalogDatabase(root.resolve("catalog"))
        try {
            val repository = RoomProjectCatalogRepository(
                database = database,
                canonicalizer = DesktopProjectRootCanonicalizer(),
                idGenerator = { UUID.randomUUID().toString() },
                nowEpochMillis = { System.currentTimeMillis() },
            )
            block(root, repository)
        } finally {
            database.close()
            root.deleteRecursively()
        }
    }
}
