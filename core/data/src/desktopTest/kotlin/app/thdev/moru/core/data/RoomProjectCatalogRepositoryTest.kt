package app.thdev.moru.core.data

import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.WorkContextDescriptor
import app.thdev.moru.core.model.WorkContextPersistedLifecycle
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomProjectCatalogRepositoryTest {
    /** Deterministic canonicalizer: canonical path is the trimmed input, always available. */
    private val fakeCanonicalizer = ProjectRootCanonicalizer { rootPath ->
        CanonicalRoot(
            lastKnownPath = rootPath,
            canonicalPath = rootPath.trimEnd('/'),
            availability = ProjectAvailability.Available,
        )
    }

    private fun repository(database: MoruCatalogDatabase): RoomProjectCatalogRepository {
        val ids = AtomicInteger(0)
        val clock = AtomicInteger(1_000)
        return RoomProjectCatalogRepository(
            database = database,
            canonicalizer = fakeCanonicalizer,
            idGenerator = { "id-${ids.incrementAndGet()}" },
            nowEpochMillis = { clock.addAndGet(1).toLong() },
        )
    }

    @Test
    fun registerProjectPersistsAndLoadsWithPrimaryRoot() {
        withCatalog { database ->
            val repository = repository(database)

            val registration = repository.registerProject("Alpha", "/work/alpha")

            assertTrue(!registration.alreadyRegistered)
            val snapshot = repository.loadCatalog()
            assertEquals(1, snapshot.projects.size)
            val project = snapshot.projects.single().project
            assertEquals("Alpha", project.displayName)
            assertEquals("/work/alpha", project.primaryRoot?.canonicalPath)
            assertEquals(ProjectAvailability.Available, project.availability)
        }
    }

    @Test
    fun duplicateCanonicalRootReturnsExistingProjectInsteadOfDuplicating() {
        withCatalog { database ->
            val repository = repository(database)
            val first = repository.registerProject("Alpha", "/work/alpha")

            val second = repository.registerProject("Alpha Again", "/work/alpha/")

            assertTrue(second.alreadyRegistered)
            assertEquals(first.project.id, second.project.id)
            assertEquals(1, repository.loadCatalog().projects.size)
        }
    }

    @Test
    fun tasksAndSelectionSurviveRestartAndStayScopedToTheirProject() {
        withTempDir { tempDir ->
            val projectAlphaId: String
            val taskId: String

            val firstDatabase = defaultDesktopMoruCatalogDatabase(tempDir)
            try {
                val repository = repository(firstDatabase)
                projectAlphaId = repository.registerProject("Alpha", "/work/alpha").project.id
                val projectBetaId = repository.registerProject("Beta", "/work/beta").project.id

                taskId = repository.createTask(projectAlphaId, "Fix login", "Login is broken").id
                repository.createTask(projectBetaId, "Update docs", "Docs are stale")
                repository.updateSelection(projectAlphaId, taskId)
            } finally {
                firstDatabase.close()
            }

            val secondDatabase = defaultDesktopMoruCatalogDatabase(tempDir)
            try {
                val snapshot = repository(secondDatabase).loadCatalog()

                assertEquals(projectAlphaId, snapshot.selectedProjectId)
                assertEquals(taskId, snapshot.selectedTaskId)
                val alpha = snapshot.projects.single { entry -> entry.project.id == projectAlphaId }
                assertEquals(listOf("Fix login"), alpha.tasks.map { task -> task.title })
                val beta = snapshot.projects.single { entry -> entry.project.displayName == "Beta" }
                assertEquals(listOf("Update docs"), beta.tasks.map { task -> task.title })
                assertEquals(taskId, alpha.project.lastSelectedTaskId)
            } finally {
                secondDatabase.close()
            }
        }
    }

    @Test
    fun loadTaskReturnsCurrentRevisionOutcomeAndStatus() {
        withCatalog { database ->
            val repository = repository(database)
            val projectId = repository.registerProject("Alpha", "/work/alpha").project.id
            val task = repository.createTask(projectId, "Fix login", "Login is broken on retry")

            val detail = repository.loadTask(task.id)

            assertEquals("Fix login", detail?.title)
            assertEquals("Login is broken on retry", detail?.desiredOutcome)
            assertEquals(1, detail?.revisionOrdinal)
        }
    }

    @Test
    fun loadTaskReturnsNullForUnknownTask() {
        withCatalog { database ->
            assertNull(repository(database).loadTask("missing-task"))
        }
    }

    @Test
    fun workContextIsPersistedAndRestoredAcrossDatabaseInstances() {
        withTempDir { tempDir ->
            val descriptor = WorkContextDescriptor(
                id = "wc-1",
                taskId = "task-1",
                persistedLifecycle = WorkContextPersistedLifecycle.Hibernated,
                branch = "feature/login",
                worktreePath = "/work/alpha/.worktrees/task-1",
            )

            val firstDatabase = defaultDesktopMoruCatalogDatabase(tempDir)
            try {
                repository(firstDatabase).saveWorkContext(descriptor)
            } finally {
                firstDatabase.close()
            }

            val secondDatabase = defaultDesktopMoruCatalogDatabase(tempDir)
            try {
                val restored = repository(secondDatabase).loadWorkContext("task-1")
                assertEquals(descriptor, restored)
            } finally {
                secondDatabase.close()
            }
        }
    }

    @Test
    fun loadWorkContextReturnsNullForTaskWithoutOne() {
        withCatalog { database ->
            assertNull(repository(database).loadWorkContext("task-none"))
        }
    }

    @Test
    fun loadCatalogSurfacesWorkContextLifecyclePerTask() {
        withCatalog { database ->
            val repository = repository(database)
            val projectId = repository.registerProject("Alpha", "/work/alpha").project.id
            val task = repository.createTask(projectId, "Fix login", "")
            repository.saveWorkContext(
                WorkContextDescriptor(
                    id = "wc-${task.id}",
                    taskId = task.id,
                    persistedLifecycle = WorkContextPersistedLifecycle.Hibernated,
                ),
            )

            val summary = repository.loadCatalog().projects.single().tasks.single()

            assertEquals(WorkContextPersistedLifecycle.Hibernated, summary.workContextLifecycle)
        }
    }

    @Test
    fun desktopRepositoryRegistersAndReloadsARealDirectoryWithTheRealCanonicalizer() {
        withTempDir { appDataDir ->
            withTempDir { projectDir ->
                val database = defaultDesktopMoruCatalogDatabase(appDataDir)
                try {
                    val realRepository = RoomProjectCatalogRepository(
                        database = database,
                        canonicalizer = DesktopProjectRootCanonicalizer(),
                        idGenerator = { "id-${java.util.UUID.randomUUID()}" },
                        nowEpochMillis = { 1_000L },
                    )

                    val registration = realRepository.registerProject("", projectDir.absolutePath)

                    assertTrue(!registration.alreadyRegistered)
                    val snapshot = realRepository.loadCatalog()
                    assertEquals(1, snapshot.projects.size)
                    assertEquals(
                        projectDir.canonicalPath,
                        snapshot.projects.single().project.primaryRoot?.canonicalPath,
                    )
                    assertEquals(ProjectAvailability.Available, snapshot.projects.single().project.availability)
                } finally {
                    database.close()
                }
            }
        }
    }

    @Test
    fun staleSelectionIsDroppedWhenReferencedIdsAreGone() {
        withCatalog { database ->
            val repository = repository(database)
            repository.registerProject("Alpha", "/work/alpha")

            repository.updateSelection("missing-project", "missing-task")

            val snapshot = repository.loadCatalog()
            assertNull(snapshot.selectedProjectId)
            assertNull(snapshot.selectedTaskId)
        }
    }

    @Test
    fun retitlingTasksKeepsTheirRecencyOrderAndCreationTime() {
        withCatalog { database ->
            val repository = repository(database)
            val projectId = repository.registerProject("Alpha", "/work/alpha").project.id
            val older = repository.createTask(projectId, "Codex session", "").id
            val newer = repository.createTask(projectId, "Fix login", "").id
            val before = repository.loadCatalog().projects.single().tasks

            repository.retitleTasks(mapOf(older to "Alpha task 1", "missing" to "Ignored"))

            val after = repository.loadCatalog().projects.single().tasks
            assertEquals(listOf(newer, older), after.map { it.id })
            assertEquals(listOf("Fix login", "Alpha task 1"), after.map { it.title })
            assertEquals(before.map { it.updatedAtEpochMillis }, after.map { it.updatedAtEpochMillis })
            assertTrue(after.single { it.id == older }.createdAtEpochMillis < after.single { it.id == newer }.createdAtEpochMillis)
        }
    }

    private fun withCatalog(block: (MoruCatalogDatabase) -> Unit) {
        withTempDir { tempDir ->
            val database = defaultDesktopMoruCatalogDatabase(tempDir)
            try {
                block(database)
            } finally {
                database.close()
            }
        }
    }

    private fun withTempDir(block: (File) -> Unit) {
        val tempDir = Files.createTempDirectory("moru-catalog-test")
        try {
            block(tempDir.toFile())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
