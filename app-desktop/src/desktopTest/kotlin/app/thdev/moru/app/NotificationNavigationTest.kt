package app.thdev.moru.app

import app.thdev.moru.core.data.*
import app.thdev.moru.core.model.TaskNotification
import java.nio.file.Files
import kotlin.test.*

class NotificationNavigationTest {
    @Test
    fun historicalReceiptSelectsCurrentOwningProjectAndTaskAndMissingOwnerDoesNotRetarget() {
        val directory = Files.createTempDirectory("moru-notification-navigation").toFile()
        val alpha = directory.resolve("Alpha").apply { mkdir() }
        val beta = directory.resolve("Beta").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory)
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val a = repository.registerProject("Alpha", alpha.path).project
            val b = repository.registerProject("Beta", beta.path).project
            val taskA = repository.createTask(a.id, "Task A", "")
            val taskB = repository.createTask(b.id, "Task B", "")
            repository.updateSelection(a.id, taskA.id)
            val navigation = WorkbenchNavigation(repository)
            val receipt = TaskNotification("n", b.id, taskB.id, "Old name", "Old title", "Old message", 1)
            assertTrue(openNotification(receipt, navigation))
            assertEquals(b.id, navigation.sidebar.selectedProjectId)
            assertEquals(taskB.id, navigation.sidebar.selectedTaskId)
            assertEquals(beta.canonicalPath, navigation.selectedProject?.rootPath)
            assertEquals("Task B", navigation.selectedTask?.title)
            navigation.selectTask(a.id, taskA.id)
            assertFalse(openNotification(receipt.copy(projectId = a.id), navigation))
            assertEquals(taskA.id, navigation.sidebar.selectedTaskId)
            navigation.deleteTask(taskB.id)
            assertFalse(openNotification(receipt, navigation))
            assertEquals(taskA.id, navigation.sidebar.selectedTaskId)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
