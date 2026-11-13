package app.thdev.moru.app

import app.thdev.moru.app.i18n.EnglishAppStrings
import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteTaskFlowTest {
    private fun withNavigation(block: (WorkbenchNavigation, RoomProjectCatalogRepository, String) -> Unit) {
        val directory = Files.createTempDirectory("moru-delete-task").toFile()
        val root = directory.resolve("proj").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            val repository = RoomProjectCatalogRepository(
                database, DesktopProjectRootCanonicalizer(), { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis,
            )
            val projectId = repository.registerProject("proj", root.path).project.id
            val taskId = repository.createTask(projectId, "Fix login", "").id
            block(WorkbenchNavigation(repository) { EnglishAppStrings }, repository, taskId)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    private fun sessionsFor(taskId: String) = InMemoryTerminalSessionManager().also { sessions ->
        sessions.createSession(TerminalSessionSpec(title = "Claude", attachedItemId = taskId, startupCommand = "claude", hostKey = "moru-claude"))
        sessions.createSession(TerminalSessionSpec(title = "Other", attachedItemId = "another-task", hostKey = "moru-other"))
    }

    @Test
    fun theTasksTerminalsStopAndTheTaskIsRemovedAndNothingElseIsTouched() = withNavigation { navigation, repository, taskId ->
        val sessions = sessionsFor(taskId)
        val ended = mutableListOf<String>()

        navigation.deleteTaskWithSessions(taskId, sessions) { key -> ended += key }

        assertNull(repository.loadTask(taskId))
        assertEquals(listOf("Other"), sessions.listSessions().map { it.title })
        // Its program in the terminal host ends too, even though this run never showed the terminal.
        assertEquals(listOf("moru-claude"), ended)
        assertNull(navigation.notice)
    }

    @Test
    fun aHeldProgramNoTerminalNamesIsTheOnlyOneLeftToEnd() {
        val sessions = InMemoryTerminalSessionManager().also { sessions ->
            sessions.createSession(TerminalSessionSpec(title = "Claude", attachedItemId = "t1", hostKey = "moru-kept"))
        }

        assertEquals(setOf("moru-lost"), unownedHeldKeys(sessions, setOf("moru-kept", "moru-lost")))
    }
}
