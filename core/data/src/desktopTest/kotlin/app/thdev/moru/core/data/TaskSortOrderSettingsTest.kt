package app.thdev.moru.core.data

import app.thdev.moru.core.model.TaskSortOrder
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Path.Companion.toOkioPath

class TaskSortOrderSettingsTest {
    @Test
    fun olderSettingsAppendTasksAndOrderingChoicesSurviveReload() {
        val directory = Files.createTempDirectory("task-order-settings")
        try {
            val path = directory.resolve("settings.json")
            path.writeText("""{"general":{"remember_last_state":false},"window":{"width_dp":1600}}""")
            val repository = FileBackedSettingsRepository(path.toOkioPath())
            val original = repository.loadSettings()
            assertEquals(TaskSortOrder.OldestFirst, original.general.taskSortOrder)
            for (order in TaskSortOrder.entries) {
                repository.updateSettings {
                    it.copy(general = it.general.copy(
                        taskSortOrder = order,
                        projectOrder = listOf("project-b", "project-a"),
                        taskOrder = mapOf("project-a" to listOf("task-2", "task-1")),
                    ))
                }
                val reloaded = FileBackedSettingsRepository(path.toOkioPath()).loadSettings()
                assertEquals(order, reloaded.general.taskSortOrder)
                assertEquals(listOf("project-b", "project-a"), reloaded.general.projectOrder)
                assertEquals(listOf("task-2", "task-1"), reloaded.general.taskOrder["project-a"])
                assertEquals(false, reloaded.general.rememberLastState)
                assertEquals(original.window, reloaded.window)
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
