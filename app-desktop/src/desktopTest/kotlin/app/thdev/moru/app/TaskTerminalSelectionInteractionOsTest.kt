package app.thdev.moru.app

import app.thdev.moru.core.data.DesktopProjectRootCanonicalizer
import app.thdev.moru.core.data.RoomProjectCatalogRepository
import app.thdev.moru.core.data.defaultDesktopMoruCatalogDatabase
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.domain.TerminalWorkspaceRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.TerminalSettings
import app.thdev.moru.core.model.TerminalWorkspaceState
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.feature.terminal.releaseTerminalSession
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import org.junit.Assume.assumeFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The real app must agree on the Task, native terminal, tab, and sidebar row. */
class TaskTerminalSelectionInteractionOsTest {
    @Test
    fun taskSwitchAndNativeThirdPaneClickKeepSidebarAndTerminalSelectionTogether() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val directory = Files.createTempDirectory("moru-terminal-selection").toFile()
        val folder = directory.resolve("Project").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        val sessions = InMemoryTerminalSessionManager()
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val project = repository.registerProject("", folder.path).project
            val firstTask = repository.createTask(project.id, "First task", "")
            val secondTask = repository.createTask(project.id, "Second task", "")
            fun terminal(taskId: String, title: String) = sessions.createSession(TerminalSessionSpec(
                title = title, attachedItemId = taskId, shell = "/bin/sh", workingDirectory = folder.canonicalPath,
            ))
            val first = terminal(firstTask.id, "First terminal")
            val second = (1..3).map { terminal(secondTask.id, "Terminal $it") }
            repository.updateSelection(project.id, firstTask.id)
            val settings = object : SettingsRepository {
                override fun loadSettings() = AppSettings(terminal = TerminalSettings(shellCommand = "/bin/sh"))
                override fun saveSettings(settings: AppSettings) {}
            }
            val workspace = object : TerminalWorkspaceRepository {
                override fun loadWorkspaceState() = TerminalWorkspaceState()
                override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
            }
            NativeWorkbenchTestWindow(1200, 800) {
                MoruApp(folder.path, sessions, StaticRuntimeStatusRepository, settings, workspace, repository)
            }.use { ui ->
                ui.waitFor("first Task's visible terminal published to sidebar") {
                    ui.selectedTab("First terminal") && sessions.selectedSessionId() == first.id
                }
                ui.click("Task Second task")
                ui.waitFor("second Task's first terminal") { ui.selectedTab("Terminal 1") }
                ui.clickTerminalPane(2)
                assertEquals(second[2].id, sessions.selectedSessionId(), "Native click must publish its session immediately")
                ui.waitFor("native third pane and terminal tab selected") {
                    sessions.selectedSessionId() == second[2].id && ui.selectedTab("Terminal 3")
                }
                ui.waitFor("third sidebar session row selected") {
                    ui.selectedTab("Terminal, Terminal 3,", substring = true)
                }
                assertEquals(secondTask.id, repository.loadCatalog().selectedTaskId)
                assertTrue(ui.selectedTab("Task Second task"))
                assertFalse(ui.selectedTab("Terminal, Terminal 1,", substring = true))
                ui.capture("task-second-terminal-third-selected")
                ui.click("Task First task")
                ui.waitFor("first Task restored") { sessions.selectedSessionId() == first.id }
                ui.click("Task Second task")
                ui.waitFor("second Task's third terminal restored in both surfaces") {
                    sessions.selectedSessionId() == second[2].id && ui.selectedTab("Terminal 3") &&
                        ui.selectedTab("Terminal, Terminal 3,", substring = true)
                }
                assertEquals(4, sessions.listSessions().size, "Navigation preserves the original terminals")
            }
        } finally {
            sessions.listSessions().forEach { releaseTerminalSession(it.id) }
            database.close()
            directory.deleteRecursively()
        }
    }
}
