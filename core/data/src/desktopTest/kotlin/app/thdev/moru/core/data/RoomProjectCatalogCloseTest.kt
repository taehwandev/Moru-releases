package app.thdev.moru.core.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import app.thdev.moru.core.model.ProjectAvailability
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Closing a Project hides it and keeps its Tasks; the same folder opened again restores it. */
class RoomProjectCatalogCloseTest {
    private val canonicalizer = ProjectRootCanonicalizer { rootPath ->
        CanonicalRoot(rootPath, rootPath.trimEnd('/'), ProjectAvailability.Available)
    }

    private fun repository(database: MoruCatalogDatabase): RoomProjectCatalogRepository {
        val ids = AtomicInteger(0)
        val clock = AtomicInteger(1_000)
        return RoomProjectCatalogRepository(
            database = database,
            canonicalizer = canonicalizer,
            idGenerator = { "id-${ids.incrementAndGet()}" },
            nowEpochMillis = { clock.addAndGet(1).toLong() },
        )
    }

    @Test
    fun aClosedProjectLeavesTheCatalogAndTheSelectionPointingAtItIsCleared() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val keep = repository.registerProject("Keep", "/work/keep").project.id
            val closing = repository.registerProject("Closing", "/work/closing").project.id
            val task = repository.createTask(closing, "Fix login", "")
            repository.updateSelection(closing, task.id)

            repository.closeProject(closing)

            val snapshot = repository.loadCatalog()
            assertEquals(listOf(keep), snapshot.projects.map { entry -> entry.project.id })
            assertNull(snapshot.selectedProjectId)
            assertNull(snapshot.selectedTaskId)
        } finally {
            database.close()
        }
    }

    @Test
    fun theMostRecentlyClosedProjectsAreOfferedForReopeningAndLeaveTheListWhenReopened() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val first = repository.registerProject("First", "/work/first").project.id
            val second = repository.registerProject("Second", "/work/second").project.id
            repository.closeProject(first)
            repository.closeProject(second)

            val closed = repository.loadCatalog().closedProjects

            assertEquals(listOf("Second", "First"), closed.map { project -> project.displayName })
            assertEquals("/work/second", closed.first().primaryRoot?.canonicalPath)

            repository.registerProject("Second", "/work/second")
            assertEquals(listOf("First"), repository.loadCatalog().closedProjects.map { project -> project.displayName })
        } finally {
            database.close()
        }
    }

    @Test
    fun deletingAClosedProjectRemovesItWithItsTasksAndTheSelectionIntoThemButNeverTheFolder() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val keep = repository.registerProject("Keep", "/work/keep").project.id
            val gone = repository.registerProject("Gone", "/work/gone").project.id
            val keptTask = repository.createTask(keep, "Stay", "outcome").id
            val goneTask = repository.createTask(gone, "Leave", "outcome").id
            repository.saveWorkContext(
                app.thdev.moru.core.model.WorkContextDescriptor(
                    id = "wc-$goneTask", taskId = goneTask,
                    persistedLifecycle = app.thdev.moru.core.model.WorkContextPersistedLifecycle.Hibernated,
                ),
            )
            repository.updateSelection(gone, goneTask)
            repository.closeProject(gone)

            repository.forgetClosedProject(gone)

            val snapshot = repository.loadCatalog()
            assertTrue(snapshot.closedProjects.isEmpty())
            assertEquals(listOf(keptTask), snapshot.projects.single().tasks.map { task -> task.id })
            assertNull(repository.loadTask(goneTask))
            assertNull(repository.loadWorkContext(goneTask))
            assertNull(snapshot.selectedProjectId)
            assertNull(snapshot.selectedTaskId)
            // The same folder opened again is a new Project with none of the old Tasks.
            val reopened = repository.registerProject("Gone", "/work/gone")
            assertEquals(false, reopened.alreadyRegistered)
            assertTrue(repository.loadCatalog().projects.first { entry -> entry.project.id == reopened.project.id }.tasks.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun anOpenOrUnknownProjectCannotBeDeletedFromTheArchive() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val open = repository.registerProject("Open", "/work/open").project.id
            repository.createTask(open, "Still here", "")

            repository.forgetClosedProject(open)
            repository.forgetClosedProject("no-such-project")

            assertEquals(listOf("Still here"), repository.loadCatalog().projects.single().tasks.map { task -> task.title })
        } finally {
            database.close()
        }
    }

    @Test
    fun renamingChangesOnlyTheDisplayNameAndSurvivesCloseAndReopen() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val id = repository.registerProject("alpha", "/work/alpha").project.id
            repository.createTask(id, "Keep me", "")

            repository.renameProject(id, "  Alpha\napp  ")

            val project = repository.loadCatalog().projects.single()
            assertEquals("Alpha app", project.project.displayName)
            assertEquals("/work/alpha", project.project.primaryRoot?.canonicalPath)
            assertEquals(listOf("Keep me"), project.tasks.map { task -> task.title })

            repository.closeProject(id)
            repository.registerProject("alpha", "/work/alpha")
            assertEquals("Alpha app", repository.loadCatalog().projects.single().project.displayName)
        } finally {
            database.close()
        }
    }

    @Test
    fun aBlankOrUnknownRenameChangesNothingAndALongNameIsCut() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val id = repository.registerProject("Alpha", "/work/alpha").project.id

            repository.renameProject(id, "   ")
            repository.renameProject("no-such-project", "Other")
            assertEquals("Alpha", repository.loadCatalog().projects.single().project.displayName)

            repository.renameProject(id, "x".repeat(200))
            assertEquals(80, repository.loadCatalog().projects.single().project.displayName.length)
        } finally {
            database.close()
        }
    }

    @Test
    fun aTaskCanBeMarkedDoneRenamedAndDeletedWithItsRevisionAndWorkContext() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val projectId = repository.registerProject("Alpha", "/work/alpha").project.id
            val keep = repository.createTask(projectId, "Keep", "keep outcome").id
            val task = repository.createTask(projectId, "Fix login", "outcome").id
            repository.saveWorkContext(
                app.thdev.moru.core.model.WorkContextDescriptor(
                    id = "wc-$task", taskId = task,
                    persistedLifecycle = app.thdev.moru.core.model.WorkContextPersistedLifecycle.Hibernated,
                ),
            )
            repository.updateSelection(projectId, task)

            repository.updateTaskStatus(task, app.thdev.moru.core.model.TaskStatus.Done)
            repository.renameTask(task, "  Fix\nsignup  ")
            repository.renameTask(task, "   ")
            val tasks = repository.loadCatalog().projects.single().tasks.associateBy { it.id }
            assertEquals(app.thdev.moru.core.model.TaskStatus.Done, tasks.getValue(task).status)
            assertEquals("Fix signup", tasks.getValue(task).title)

            repository.deleteTask(task)

            val after = repository.loadCatalog()
            assertEquals(listOf(keep), after.projects.single().tasks.map { it.id })
            assertNull(after.selectedTaskId)
            assertNull(repository.loadTask(task))
            assertNull(repository.loadWorkContext(task))
            assertEquals("keep outcome", repository.loadTask(keep)?.desiredOutcome)
            repository.deleteTask("no-such-task")
        } finally {
            database.close()
        }
    }

    @Test
    fun aTaskRemembersItsAgentAndForgetsItOnRequestAndTheNewestUseWins() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val projectId = repository.registerProject("Alpha", "/work/alpha").project.id
            val first = repository.createTask(projectId, "First", "").id
            val second = repository.createTask(projectId, "Second", "").id

            repository.setTaskAgent(first, "Claude")
            repository.setTaskAgent(second, "Codex")
            val agents = repository.loadCatalog().projects.single().tasks.associate { it.id to it.lastAgent }
            assertEquals("Claude", agents.getValue(first))
            assertEquals("Codex", agents.getValue(second))

            repository.setTaskAgent(first, "  ")
            repository.setTaskAgent("no-such-task", "Claude")
            assertNull(repository.loadCatalog().projects.single().tasks.first { it.id == first }.lastAgent)
        } finally {
            database.close()
        }
    }

    @Test
    fun closingAnotherProjectLeavesTheSelectionAlone() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val keep = repository.registerProject("Keep", "/work/keep").project.id
            val closing = repository.registerProject("Closing", "/work/closing").project.id
            repository.updateSelection(keep, null)

            repository.closeProject(closing)

            assertEquals(keep, repository.loadCatalog().selectedProjectId)
        } finally {
            database.close()
        }
    }

    @Test
    fun registeringTheSameFolderAgainRestoresTheClosedProjectWithItsTasks() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            val id = repository.registerProject("Alpha", "/work/alpha").project.id
            repository.createTask(id, "Keep me", "Outcome")
            repository.closeProject(id)

            val registration = repository.registerProject("Alpha", "/work/alpha/")

            assertTrue(registration.alreadyRegistered)
            assertEquals(id, registration.project.id)
            val entry = repository.loadCatalog().projects.single()
            assertEquals(listOf("Keep me"), entry.tasks.map { task -> task.title })
        } finally {
            database.close()
        }
    }

    @Test
    fun closingAnUnknownProjectIsIgnored() = withTempDir { dir ->
        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val repository = repository(database)
            repository.registerProject("Alpha", "/work/alpha")

            repository.closeProject("no-such-project")

            assertEquals(1, repository.loadCatalog().projects.size)
        } finally {
            database.close()
        }
    }

    @Test
    fun aVersionTwoCatalogMigratesWithItsProjectsAndTasksIntact() = withTempDir { dir ->
        val file = dir.resolve("catalog").apply { mkdirs() }.resolve("moru-catalog.db")
        createVersionTwoDatabase(file)

        val database = defaultDesktopMoruCatalogDatabase(dir)
        try {
            val snapshot = repository(database).loadCatalog()

            val entry = snapshot.projects.single()
            assertEquals("Old project", entry.project.displayName)
            assertEquals(listOf("Old task"), entry.tasks.map { task -> task.title })
            // The new column starts open, so nothing the user already had disappears.
            repository(database).closeProject(entry.project.id)
            assertTrue(repository(database).loadCatalog().projects.isEmpty())
        } finally {
            database.close()
        }
    }

    /** Builds the exact v2 schema from the exported schema file, then adds one project with a task. */
    private fun createVersionTwoDatabase(file: File) {
        val schema = File("schemas/app.thdev.moru.core.data.MoruCatalogDatabase/2.json").readText()
        fun unescape(raw: String) = Regex("""\\u([0-9a-fA-F]{4})""").replace(raw) { match ->
            match.groupValues[1].toInt(16).toChar().toString()
        }.replace("\\\"", "\"").replace("\\\\", "\\")
        val tableNames = Regex(""""tableName":\s*"([^"]+)"""").findAll(schema).map { it.groupValues[1] }.toList()
        val statements = Regex(""""createSql":\s*"((?:[^"\\]|\\.)*)"""").findAll(schema).map { match ->
            unescape(match.groupValues[1])
        }.toList()
        val setup = Regex(""""setupQueries":\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(schema)!!.groupValues[1]
        val setupQueries = Regex(""""((?:[^"\\]|\\.)*)"""").findAll(setup).map { unescape(it.groupValues[1]) }.toList()

        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            // Each entity's create statements use a table-name placeholder; indexes follow their own table.
            var current = 0
            statements.forEach { sql ->
                val table = if (sql.startsWith("CREATE TABLE")) tableNames[current++] else tableNames[current - 1]
                connection.execSQL(sql.replace("\${TABLE_NAME}", table))
            }
            setupQueries.forEach { connection.execSQL(it) }
            connection.execSQL("INSERT INTO catalog_project (id, display_name, last_selected_task_id, created_at, updated_at) VALUES ('p1', 'Old project', NULL, 1, 1)")
            connection.execSQL("INSERT INTO catalog_project_root (id, project_id, role, last_known_path, canonical_path, availability, sort_order) VALUES ('r1', 'p1', 'primary', '/old', '/old', 'available', 0)")
            connection.execSQL("INSERT INTO catalog_task (id, project_id, title, status, current_revision_id, current_result_id, created_at, updated_at) VALUES ('t1', 'p1', 'Old task', 'understanding', 'rev1', NULL, 1, 1)")
            connection.execSQL("PRAGMA user_version = 2")
        } finally {
            connection.close()
        }
    }

    private fun withTempDir(block: (File) -> Unit) {
        val tempDir = Files.createTempDirectory("moru-catalog-close-test")
        try {
            block(tempDir.toFile())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
