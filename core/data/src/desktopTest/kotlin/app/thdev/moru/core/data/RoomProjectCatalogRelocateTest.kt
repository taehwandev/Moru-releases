package app.thdev.moru.core.data

import app.thdev.moru.core.domain.ProjectRelocation
import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.WorkContextDescriptor
import app.thdev.moru.core.model.WorkContextPersistedLifecycle
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A Project whose folder was renamed or moved is pointed at its new folder and keeps its Tasks. */
class RoomProjectCatalogRelocateTest {
    /** Paths in [missing] are gone from disk; everything else is there. */
    private val missing = mutableSetOf<String>()
    private val canonicalizer = ProjectRootCanonicalizer { rootPath ->
        val available = rootPath.trimEnd('/') !in missing
        CanonicalRoot(
            lastKnownPath = rootPath,
            canonicalPath = rootPath.trimEnd('/').takeIf { available },
            availability = if (available) ProjectAvailability.Available else ProjectAvailability.Missing,
        )
    }

    private fun repository(database: MoruCatalogDatabase): RoomProjectCatalogRepository {
        val ids = AtomicInteger(0)
        val clock = AtomicInteger(1_000)
        return RoomProjectCatalogRepository(database, canonicalizer, { "id-${ids.incrementAndGet()}" }, { clock.addAndGet(1).toLong() })
    }

    @Test
    fun aRenamedFolderShowsMissingAndRelocatingKeepsTheProjectAndItsTasks() {
        withCatalog { repository ->
            val projectId = repository.registerProject("Alpha", "/work/alpha").project.id
            val task = repository.createTask(projectId, "Fix login", "")
            repository.saveWorkContext(
                WorkContextDescriptor("wc-1", task.id, WorkContextPersistedLifecycle.Hibernated, "fix", "/work/alpha/.worktrees/fix"),
            )

            missing += "/work/alpha"
            assertEquals(ProjectAvailability.Missing, repository.loadCatalog().projects.single().project.availability)

            assertEquals(ProjectRelocation.Moved, repository.relocateProject(projectId, "/work/alpha-renamed"))

            val entry = repository.loadCatalog().projects.single()
            assertEquals(projectId, entry.project.id)
            assertEquals("Alpha", entry.project.displayName)
            assertEquals("/work/alpha-renamed", entry.project.primaryRoot?.canonicalPath)
            assertEquals(ProjectAvailability.Available, entry.project.availability)
            assertEquals(listOf(task.id), entry.tasks.map { it.id })
            assertEquals("/work/alpha-renamed/.worktrees/fix", repository.loadWorkContext(task.id)?.worktreePath)
            // Opening the new folder finds the same Project rather than making another.
            assertTrue(repository.registerProject("", "/work/alpha-renamed").alreadyRegistered)
        }
    }

    @Test
    fun aFolderThatIsAnotherProjectOrIsGoneIsRefused() {
        withCatalog { repository ->
            val alpha = repository.registerProject("Alpha", "/work/alpha").project.id
            repository.registerProject("Beta", "/work/beta")
            missing += "/work/nowhere"

            assertEquals(ProjectRelocation.FolderInUse, repository.relocateProject(alpha, "/work/beta"))
            assertEquals(ProjectRelocation.FolderUnavailable, repository.relocateProject(alpha, "/work/nowhere"))
            assertEquals(ProjectRelocation.NotFound, repository.relocateProject("no-such-project", "/work/gamma"))
            assertEquals(ProjectRelocation.Moved, repository.relocateProject(alpha, "/work/alpha/"))

            val roots = repository.loadCatalog().projects.associate { it.project.displayName to it.project.primaryRoot?.canonicalPath }
            assertEquals(mapOf("Alpha" to "/work/alpha", "Beta" to "/work/beta"), roots)
        }
    }

    private fun withCatalog(block: (RoomProjectCatalogRepository) -> Unit) {
        val tempDir = Files.createTempDirectory("moru-catalog-relocate").toFile()
        val database = defaultDesktopMoruCatalogDatabase(tempDir)
        try {
            block(repository(database))
        } finally {
            database.close()
            tempDir.deleteRecursively()
        }
    }
}
