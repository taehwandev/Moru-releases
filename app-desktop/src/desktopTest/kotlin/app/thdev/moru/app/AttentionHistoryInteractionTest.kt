package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.data.*
import app.thdev.moru.core.model.TaskNotification
import app.thdev.moru.core.system.AttentionNotifier
import java.nio.file.Files
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class AttentionHistoryInteractionTest {
    @Test
    fun systemAlertsOffAndViewedTaskStillRecordOnceAndDeletingHistoryDoesNotResetTheEpisode() {
        val directory = Files.createTempDirectory("moru-attention-history").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory)
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val project = repository.registerProject("Example", directory.path).project
            val task = repository.createTask(project.id, "Example task", "")
            repository.updateSelection(project.id, task.id)
            val navigation = WorkbenchNavigation(repository)
            val failed = AttentionCandidate(task.id, task.title, AttentionKind.Failed)
            var attention by mutableStateOf(AttentionState(listOf(failed)))
            val history = mutableListOf<TaskNotification>()
            var systemAlerts = 0
            val notifier = object : AttentionNotifier {
                override fun badge(count: Int) {}
                override fun show(title: String, body: String) { systemAlerts++ }
            }
            runDesktopComposeUiTest(width = 400, height = 300) {
                setContent {
                    AttentionAlerts(navigation, attention, notifier, notifications = false,
                        onRecord = { history += it }, viewedTaskId = task.id)
                }
                waitForIdle()
                runOnIdle {
                    assertEquals(1, history.size)
                    assertEquals(project.id, history.single().projectId)
                    assertEquals(task.id, history.single().taskId)
                    assertEquals("Example task failed.", history.single().message)
                    assertTrue(history.single().createdAtMillis > 0)
                    assertTrue(history.single().read, "A Task on screen in a focused window is recorded already read")
                    history.clear()
                    attention = AttentionState(listOf(failed.copy(title = "Renamed task")))
                }
                waitForIdle()
                runOnIdle {
                    assertTrue(history.isEmpty(), "Deleting history must not reannounce an ongoing episode")
                    attention = AttentionState(listOf(failed.copy(kind = AttentionKind.Asking, question = "Proceed?")))
                }
                waitForIdle()
                runOnIdle {
                    assertEquals(1, history.size, "A new attention reason creates a new receipt")
                    assertEquals(0, systemAlerts)
                }
            }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun anotherTasksAttentionIsUnreadAndAlertsAndLookingAtItReadsItAndTheIconFollowsTheUnreadCount() {
        val directory = Files.createTempDirectory("moru-attention-read").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory)
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val project = repository.registerProject("Example", directory.path).project
            val viewed = repository.createTask(project.id, "On screen", "")
            val other = repository.createTask(project.id, "Elsewhere", "")
            val navigation = WorkbenchNavigation(repository)
            val history = mutableListOf<TaskNotification>()
            val read = mutableListOf<Set<String>>()
            val badges = mutableListOf<Int>()
            var alerts = 0
            val notifier = object : AttentionNotifier {
                override fun badge(count: Int) { badges += count }
                override fun show(title: String, body: String) { alerts++ }
            }
            var viewedTaskId by mutableStateOf(viewed.id)
            var unread by mutableStateOf(0)
            runDesktopComposeUiTest(width = 400, height = 300) {
                setContent {
                    AttentionAlerts(navigation, AttentionState(listOf(AttentionCandidate(other.id, other.title, AttentionKind.Failed))),
                        notifier, notifications = true, onRecord = { history += it }, viewedTaskId = viewedTaskId,
                        unreadCount = unread, onTasksRead = { read += it })
                }
                waitForIdle()
                runOnIdle {
                    assertEquals(listOf(false), history.map { it.read })
                    assertEquals(1, alerts)
                    assertTrue(read.none { other.id in it })
                    unread = 1
                }
                waitForIdle()
                runOnIdle {
                    assertEquals(1, badges.last())
                    viewedTaskId = other.id
                }
                waitForIdle()
                runOnIdle { assertTrue(read.any { other.id in it }, "Looking at the Task marks its record read") }
            }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
