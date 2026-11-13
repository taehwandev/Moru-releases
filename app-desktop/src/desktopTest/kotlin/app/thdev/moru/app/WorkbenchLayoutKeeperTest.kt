package app.thdev.moru.app

import app.thdev.moru.core.data.InMemoryWorkbenchLayoutRepository
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.model.WorkbenchLayout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkbenchLayoutKeeperTest {
    @Test
    fun aChangeIsWrittenSoonAfterItIsMadeWithoutWaitingForTheWindowToClose() {
        val repository = InMemoryWorkbenchLayoutRepository()
        val keeper = WorkbenchLayoutKeeper(repository, saveDelayMillis = 50)

        keeper.update { it.copy(collapsedProjectIds = listOf("p1")) }
        keeper.update { it.copy(collapsedProjectIds = listOf("p1", "p2")) }

        val deadline = System.currentTimeMillis() + 3_000
        while (repository.loadLayout().collapsedProjectIds != listOf("p1", "p2") && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertEquals(listOf("p1", "p2"), repository.loadLayout().collapsedProjectIds)
    }

    @Test
    fun aChangeMadeJustBeforeQuitIsWrittenByFlush() {
        val repository = InMemoryWorkbenchLayoutRepository()
        val keeper = WorkbenchLayoutKeeper(repository, saveDelayMillis = 60_000)

        keeper.update { it.copy(collapsedProjectIds = listOf("p1")) }
        keeper.flush()

        assertEquals(listOf("p1"), repository.loadLayout().collapsedProjectIds)
    }

    @Test
    fun aCollapsedProjectStaysCollapsedAndEveryOtherOneStartsExpanded() {
        val directory = Files.createTempDirectory("moru-layout").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            val repository = RoomProjectCatalogRepository(
                database, DesktopProjectRootCanonicalizer(),
                idGenerator = { java.util.UUID.randomUUID().toString() }, nowEpochMillis = System::currentTimeMillis,
            )
            val alpha = directory.resolve("alpha").apply { mkdir() }
            val beta = directory.resolve("beta").apply { mkdir() }
            val first = WorkbenchNavigation(repository)
            first.openProject(alpha.path)
            first.openProject(beta.path)
            val alphaId = first.sidebar.projects.first { it.displayName == "alpha" }.id
            val betaId = first.sidebar.projects.first { it.displayName == "beta" }.id
            assertEquals(true, first.sidebar.projects.all { it.expanded })
            first.toggleProject(alphaId)
            val saved = WorkbenchLayout(collapsedProjectIds = first.collapsedProjects().sorted())

            val next = WorkbenchNavigation(repository, initiallyCollapsed = saved.collapsedProjectIds.toSet())

            assertEquals(false, next.sidebar.projects.first { it.id == alphaId }.expanded)
            assertEquals(true, next.sidebar.projects.first { it.id == betaId }.expanded)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
