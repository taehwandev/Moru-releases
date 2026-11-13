package app.thdev.moru.app

import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.feature.terminal.releaseTerminalSession
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

class NotificationInboxAppInteractionOsTest {
    @Test
    fun bellOpensHistoryPanelAndAReceiptNavigatesToItsCurrentProjectAndTask() {
        val directory = Files.createTempDirectory("moru-notification-app").toFile()
        val alpha = directory.resolve("Alpha").apply { mkdir() }
        val beta = directory.resolve("Beta").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory)
        val sessions = InMemoryTerminalSessionManager()
        try {
            val catalog = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val a = catalog.registerProject("Alpha", alpha.path).project
            val b = catalog.registerProject("Beta", beta.path).project
            val taskA = catalog.createTask(a.id, "Task A", "")
            val taskB = catalog.createTask(b.id, "Task B", "")
            catalog.updateSelection(a.id, taskA.id)
            val notifications = InMemoryNotificationRepository()
            val receipt = TaskNotification("receipt", b.id, taskB.id, "Beta", "Task B", "Task B needs you.", 1)
            runBlocking { notifications.append(listOf(receipt)) }
            var selectedRoot: String? = null
            val settings = object : SettingsRepository {
                override fun loadSettings() = AppSettings(terminal = TerminalSettings(shellCommand = "/bin/sh"))
                override fun saveSettings(settings: AppSettings) {}
            }
            val workspace = object : TerminalWorkspaceRepository {
                override fun loadWorkspaceState() = TerminalWorkspaceState()
                override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
            }
            NativeWorkbenchTestWindow(1100, 760) {
                MoruApp(alpha.path, sessions, StaticRuntimeStatusRepository,
                    settings, workspace, catalog, notificationRepository = notifications,
                    onSelectProjectRoot = { selectedRoot = it })
            }.use { ui ->
                ui.click("Notifications")
                ui.waitFor("inbox panel") { ui.exists("Mark all read") }
                ui.capture("notification-inbox-popover")
                ui.pressEscapeInPopover()
                ui.waitFor("inbox panel closed by Escape") { !ui.exists("Mark all read") }
                ui.click("Notifications")
                ui.click("Open task: Task B in Beta")
                ui.waitFor("notification owning root") { selectedRoot == beta.canonicalPath }
                ui.waitFor("inbox panel closed by opening its Task") { !ui.exists("Mark all read") }
                assertEquals(b.id, catalog.loadCatalog().selectedProjectId)
                assertEquals(taskB.id, catalog.loadCatalog().selectedTaskId)
                ui.waitFor("read receipt") { runBlocking { notifications.load() }.single().read }
                ui.click("Notifications")
                ui.capture("notification-inbox-app")
                ui.click("Delete notification for Task B in Beta")
                ui.waitFor("deleted receipt") { runBlocking { notifications.load() }.isEmpty() }
                assertEquals(taskB.id, catalog.loadCatalog().selectedTaskId)
                assertEquals(2, catalog.loadCatalog().projects.sumOf { it.tasks.size })
            }
        } finally {
            sessions.listSessions().forEach { releaseTerminalSession(it.id) }
            database.close()
            directory.deleteRecursively()
        }
    }
}
