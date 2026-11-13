package app.thdev.moru.app

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.TaskSortOrder
import app.thdev.moru.feature.settings.SettingsEditorState
import app.thdev.moru.feature.settings.SettingsSaveStatus
import app.thdev.moru.feature.settings.rememberSettingsEditorState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SidebarOrderChangesTest {
    @Test
    fun validSiblingMovesSaveAndApplyButInvalidMovesAndFailedSavesDoNot() {
        val directory = Files.createTempDirectory("sidebar-order-edits").toFile()
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            var counter = 0L
            val catalog = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                idGenerator = { "id-${++counter}" }, nowEpochMillis = { ++counter })
            val alpha = catalog.registerProject("Alpha", directory.resolve("alpha").apply { mkdir() }.path).project.id
            val beta = catalog.registerProject("Beta", directory.resolve("beta").apply { mkdir() }.path).project.id
            val first = catalog.createTask(alpha, "First", "Keep request").id
            val second = catalog.createTask(alpha, "Second", "").id
            val betaTask = catalog.createTask(beta, "Beta task", "").id
            catalog.updateSelection(alpha, first)
            val navigation = WorkbenchNavigation(catalog)
            val repository = OrderSettingsRepository()
            runDesktopComposeUiTest {
                lateinit var changes: SidebarOrderChanges
                lateinit var editor: SettingsEditorState
                setContent {
                    val current = rememberSettingsEditorState(repository) { navigation.applyGeneralSettings(it.general) }
                    SideEffect { editor = current; changes = SidebarOrderChanges(navigation, current) }
                }
                runOnIdle {
                    changes.tasks(alpha, listOf(second, second))
                    changes.tasks(alpha, listOf(second, betaTask))
                    changes.tasks("missing", listOf(second, first))
                    changes.projects(listOf(beta))
                    assertEquals(SettingsSaveStatus.Ready, editor.saveStatus, "Reject stale/duplicate/foreign ids before saving")
                    changes.tasks(alpha, listOf(second, first))
                }
                waitUntil(timeoutMillis = 10_000) { editor.saveStatus == SettingsSaveStatus.Saved }
                runOnIdle {
                    assertEquals(TaskSortOrder.Manual, repository.stored.general.taskSortOrder)
                    assertEquals(listOf(second, first), navigation.selectedProject!!.tasks.map { it.id })
                    assertEquals(first, navigation.sidebar.selectedTaskId)
                    changes.projects(listOf(beta, alpha))
                }
                waitUntil(timeoutMillis = 10_000) { editor.saveStatus == SettingsSaveStatus.Saved && navigation.sidebar.projects.first().id == beta }
                runOnIdle {
                    assertEquals(listOf(second, first), repository.stored.general.taskOrder[alpha])
                    repository.fail = true
                    changes.tasks(alpha, listOf(first, second))
                }
                waitUntil(timeoutMillis = 10_000) { editor.saveStatus == SettingsSaveStatus.Failed }
                runOnIdle {
                    assertEquals(listOf(second, first), navigation.selectedProject!!.tasks.map { it.id })
                    repository.fail = false
                    editor.retry()
                }
                waitUntil(timeoutMillis = 10_000) { editor.saveStatus == SettingsSaveStatus.Saved }
                runOnIdle {
                    val restored = WorkbenchNavigation(catalog, initialGeneral = repository.stored.general)
                    assertEquals(listOf(beta, alpha), restored.sidebar.projects.map { it.id })
                    assertEquals(listOf(first, second), restored.selectedProject!!.tasks.map { it.id })
                    assertEquals("Keep request", restored.taskDescription(first))
                    assertEquals(first, restored.sidebar.selectedTaskId)
                }
            }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    private class OrderSettingsRepository : SettingsRepository {
        @Volatile var stored = AppSettings()
        @Volatile var fail = false
        override fun loadSettings() = stored
        override fun saveSettings(settings: AppSettings) {
            if (fail) error("Disk unavailable")
            stored = settings
        }
    }
}
