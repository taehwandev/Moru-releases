package app.thdev.moru.app

import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.WorkbenchLayoutRepository
import app.thdev.moru.core.model.WorkbenchLayout
import app.thdev.moru.feature.workbench.launch.AgentRuntime
import java.nio.file.Files
import kotlin.test.*

/** A Project in its own window next to the main window, both on one catalog, as the app runs them. */
class ProjectWindowCatalogTest {
    @Test
    fun aProjectWindowShowsItsProjectAndChoosesTasksWithoutMovingTheMainWindow() = withRepository { repository, root ->
        val alpha = root.resolve("Alpha").apply { mkdir() }
        val beta = root.resolve("Beta").apply { mkdir() }
        val main = WorkbenchNavigation(AnnouncingCatalog(repository))
        main.openProject(beta.path)
        val betaId = main.sidebar.selectedProjectId!!
        main.launchDirect(betaId, AgentRuntime.Codex)
        main.launchDirect(betaId, AgentRuntime.Codex)
        val betaTasks = main.sidebar.projects.single { it.id == betaId }.tasks.map { it.id }
        main.openProject(alpha.path)
        val alphaId = main.sidebar.selectedProjectId!!
        main.launchDirect(alphaId, AgentRuntime.Codex)
        val alphaTask = main.sidebar.selectedTaskId

        val window = WorkbenchNavigation(ProjectWindowCatalog(AnnouncingCatalog(repository), betaId))
        assertEquals(listOf(betaId), window.sidebar.projects.map { it.id })
        assertEquals(betaId, window.sidebar.selectedProjectId)

        val other = betaTasks.first { it != window.sidebar.selectedTaskId }
        window.selectTask(betaId, other)
        assertEquals(other, window.sidebar.selectedTaskId)

        // The main window keeps its own choice, now and after a relaunch.
        main.reload()
        assertEquals(alphaId, main.sidebar.selectedProjectId)
        assertEquals(alphaTask, main.sidebar.selectedTaskId)
        val relaunched = WorkbenchNavigation(repository)
        assertEquals(alphaTask, relaunched.sidebar.selectedTaskId)
        // Beta reopens on the Task chosen in its window.
        relaunched.selectProject(betaId)
        assertEquals(other, relaunched.sidebar.selectedTaskId)
    }

    @Test
    fun aProjectWindowRecordsItsTaskWhileTheMainWindowIsOnTheSameProject() = withRepository { repository, root ->
        val main = WorkbenchNavigation(AnnouncingCatalog(repository))
        main.openProject(root.path)
        val projectId = main.sidebar.selectedProjectId!!
        main.launchDirect(projectId, AgentRuntime.Codex)
        main.launchDirect(projectId, AgentRuntime.Codex)
        val window = WorkbenchNavigation(ProjectWindowCatalog(AnnouncingCatalog(repository), projectId))
        val other = window.sidebar.projects.single().tasks.map { it.id }.first { it != main.sidebar.selectedTaskId }

        window.selectTask(projectId, other)

        val relaunched = WorkbenchNavigation(repository)
        relaunched.selectProject(projectId)
        assertEquals(other, relaunched.sidebar.selectedTaskId)
    }

    @Test
    fun aTaskMadeInOneWindowIsAnnouncedToTheOthers() = withRepository { repository, root ->
        val mainCatalog = AnnouncingCatalog(repository)
        val main = WorkbenchNavigation(mainCatalog)
        main.openProject(root.path)
        val projectId = main.sidebar.selectedProjectId!!
        val windowCatalog = AnnouncingCatalog(repository)
        val window = WorkbenchNavigation(ProjectWindowCatalog(windowCatalog, projectId))
        val before = CatalogChanges.revision
        mainCatalog.othersChanged(before)
        windowCatalog.othersChanged(before)

        window.launchDirect(projectId, AgentRuntime.Codex)

        assertTrue(CatalogChanges.revision > before, "making a Task must tell the other windows")
        // Only the other window reads the catalog again; the one that made the Task has it already.
        assertTrue(mainCatalog.othersChanged(CatalogChanges.revision))
        assertFalse(windowCatalog.othersChanged(CatalogChanges.revision))
        main.reload()
        assertEquals(1, main.sidebar.projects.single { it.id == projectId }.tasks.size)
    }

    @Test
    fun aClosedProjectsWindowClosesAndTheOthersKeepTheirTitles() = withRepository { repository, root ->
        val alpha = root.resolve("Alpha").apply { mkdir() }
        val beta = root.resolve("Beta").apply { mkdir() }
        val main = WorkbenchNavigation(AnnouncingCatalog(repository))
        main.openProject(alpha.path)
        val alphaId = main.sidebar.selectedProjectId!!
        main.openProject(beta.path)
        val betaId = main.sidebar.selectedProjectId!!
        val windows = ProjectWindows(WorkbenchLayoutKeeper(object : WorkbenchLayoutRepository {
            override fun loadLayout() = WorkbenchLayout()
            override fun saveLayout(layout: WorkbenchLayout) = Unit
        }), repository)
        windows.open(alphaId)
        windows.open(betaId)

        repository.closeProject(betaId)
        windows.refresh()

        assertEquals(listOf(alphaId), windows.windows.map { it.projectId })
        assertEquals("Alpha", windows.names[alphaId])
    }

    @Test
    fun projectWindowsKeepOneWindowPerProjectAndWhereItWas() {
        val keeper = WorkbenchLayoutKeeper(object : WorkbenchLayoutRepository {
            override fun loadLayout() = WorkbenchLayout()
            override fun saveLayout(layout: WorkbenchLayout) = Unit
        })
        val windows = ProjectWindows(keeper)
        windows.open("p")
        windows.open("p")
        assertEquals(listOf("p"), windows.windows.map { it.projectId })
        assertEquals("p", windows.forwardRequest)
        windows.forwarded("p")
        windows.moved("p", 100f, 80f, 1200f, 800f)
        assertEquals(1200f, keeper.layout.projectWindows.single().width)
        windows.bringForward("p")
        assertEquals("p", windows.forwardRequest)
        windows.close("p")
        assertTrue(windows.windows.isEmpty())
        assertNull(windows.forwardRequest)
    }

    private fun withRepository(block: (RoomProjectCatalogRepository, java.io.File) -> Unit) {
        val directory = Files.createTempDirectory("moru-project-window").toFile()
        val root = directory.resolve("project").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            block(RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                idGenerator = { java.util.UUID.randomUUID().toString() },
                nowEpochMillis = System::currentTimeMillis), root)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
