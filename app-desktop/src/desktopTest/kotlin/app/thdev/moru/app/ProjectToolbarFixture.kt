package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.data.*
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import java.nio.file.Files

/** Real catalog/selection wiring, with a terminal view stand-in that starts no process. */
internal class ProjectToolbarFixture : AutoCloseable {
    private val directory = Files.createTempDirectory("moru-toolbar-test").toFile()
    val alpha = directory.resolve("Alpha").apply { mkdir() }
    val beta = directory.resolve("Beta").apply { mkdir() }
    val recent = directory.resolve("Recent").apply { mkdir() }
    private val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
    val catalog = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
        { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
    val projectA = catalog.registerProject("Alpha", alpha.path).project
    val projectB = catalog.registerProject("Beta", beta.path).project
    val projectRecent = catalog.registerProject("Recent", recent.path).project
    val taskA = catalog.createTask(projectA.id, "Alpha task", "Read Alpha")
    val taskB = catalog.createTask(projectB.id, "Beta task", "Read Beta")
    val sessions = InMemoryTerminalSessionManager()
    var pickedFolder: String? = null
    var pickerFailure: Boolean = false
    var selectedRoot: String? = null
    var generalSettings: GeneralSettings = GeneralSettings()

    init {
        catalog.closeProject(projectRecent.id)
        catalog.updateSelection(projectA.id, taskA.id)
    }

    @Composable
    fun App(dark: Boolean = false) {
        val settings = object : SettingsRepository {
            override fun loadSettings() = AppSettings(
                general = generalSettings,
                appearance = AppearanceSettings(theme = if (dark) AppTheme.Dark else AppTheme.Light),
            )
            override fun saveSettings(settings: AppSettings) {}
        }
        val workspace = object : TerminalWorkspaceRepository {
            override fun loadWorkspaceState() = TerminalWorkspaceState()
            override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
        }
        MoruApp(alpha.path, sessions, StaticRuntimeStatusRepository, settings, workspace, catalog,
            windowTopInset = 28.dp,
            onPickProjectFolder = { if (pickerFailure) error("Picker unavailable") else pickedFolder },
            onSelectProjectRoot = { selectedRoot = it },
            gitStatus = WorkbenchGitStatus(true, "alpha-branch", emptyList()), projectDataRoot = alpha.canonicalPath,
            paneSlots = DesktopWorkbenchPaneSlots.copy(terminal = DesktopWorkbenchPaneSlots.terminal.copy(
                pane = { _, modifier -> MoruText("Test terminal view", modifier = modifier) })))
    }

    override fun close() {
        database.close()
        directory.deleteRecursively()
    }
}
