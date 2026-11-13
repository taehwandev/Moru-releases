package app.thdev.moru.app

import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.domain.ProjectCatalogRepository
import app.thdev.moru.core.runtime.DesktopRuntimeStatusRepository
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoruDesktopModuleTest {
    @Test
    fun desktopAppGraphWiresSettingsAndRuntimeStatus() {
        val projectRoot = Files.createTempDirectory("moru-metro-spike").toFile()

        try {
            val graph = createDesktopAppGraph(projectRoot)

            assertNotNull(graph.settingsRepository.loadSettings())
            assertTrue(graph.runtimeStatusRepository is DesktopRuntimeStatusRepository)
        } finally {
            projectRoot.deleteRecursively()
        }
    }

    @Test
    fun desktopDependenciesRegisterProvidedRootWithoutStartingUnownedTerminal() = withCatalog { root, repository ->
        val project = root.resolve("project").apply { mkdirs() }

        val dependencies = createMoruDesktopDependencies(project, projectCatalogRepository = repository)

        assertEquals(project.canonicalFile, dependencies.projectRoot)
        assertEquals(project.canonicalPath, repository.loadCatalog().projects.single().project.primaryRoot?.canonicalPath)
        assertNotNull(repository.loadCatalog().selectedProjectId)
        assertTrue(dependencies.terminalSessionManager.listSessions().isEmpty())
        assertNull(dependencies.startupNotice)
    }

    @Test
    fun startupDependenciesUseRequestedProjectForStorageAndCatalog() = withCatalog { root, repository ->
        val project = root.resolve("my project").apply { mkdirs() }

        val dependencies = createMoruStartupDependencies(
            arguments = arrayOf("--project", "my project"),
            configuredPath = null,
            userDir = root,
            userHome = root.resolve("home"),
            appDataDir = root.resolve("catalog"),
            projectCatalogRepository = repository,
        )

        assertEquals(project.canonicalFile, dependencies.projectRoot)
        assertTrue(project.resolve(".moru-community").isDirectory)
        assertEquals(project.canonicalPath, repository.loadCatalog().projects.single().project.primaryRoot?.canonicalPath)
        assertTrue(dependencies.terminalSessionManager.listSessions().isEmpty())
    }

    @Test
    fun emptyGuiDependenciesKeepCatalogEmptyAndDoNotCreateUnownedSession() = withCatalog { root, repository ->
        val home = root.resolve("home").apply { mkdirs() }

        val dependencies = createMoruStartupDependencies(
            arguments = emptyArray(),
            configuredPath = null,
            userDir = home,
            userHome = home,
            appDataDir = root.resolve("catalog"),
            projectCatalogRepository = repository,
        )

        assertEquals(defaultMoruStorageRoot(home), dependencies.projectRoot)
        assertTrue(repository.loadCatalog().projects.isEmpty())
        assertTrue(dependencies.terminalSessionManager.listSessions().isEmpty())
    }

    private fun withCatalog(block: (File, ProjectCatalogRepository) -> Unit) {
        val root = Files.createTempDirectory("moru-desktop-module").toFile()
        val database = defaultDesktopMoruCatalogDatabase(root.resolve("catalog"))
        try {
            block(
                root,
                RoomProjectCatalogRepository(
                    database = database,
                    canonicalizer = DesktopProjectRootCanonicalizer(),
                    idGenerator = { UUID.randomUUID().toString() },
                    nowEpochMillis = { System.currentTimeMillis() },
                ),
            )
        } finally {
            database.close()
            root.deleteRecursively()
        }
    }
}
