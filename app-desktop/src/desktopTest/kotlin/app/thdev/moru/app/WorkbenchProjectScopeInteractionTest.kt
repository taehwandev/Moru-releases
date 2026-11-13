package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import java.nio.file.Files
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class WorkbenchProjectScopeInteractionTest {
    @Test
    fun projectSwitchCannotOpenAFileFromThePreviousProjectWhileFolderDataCatchesUp() {
        val directory = Files.createTempDirectory("moru-project-scope").toFile()
        val alpha = directory.resolve("Alpha").apply { mkdir() }
        val beta = directory.resolve("Beta").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            val repository = RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis)
            val alphaRegistration = repository.registerProject("", alpha.path)
            repository.registerProject("", beta.path)
            repository.updateSelection(alphaRegistration.project.id, null)
            var dataRoot by mutableStateOf(alpha.canonicalPath)
            var entries by mutableStateOf(listOf(WorkbenchProjectTreeEntry("alpha.txt", "alpha.txt", 0, true)))
            val opened = mutableListOf<Pair<String, String>>()
            val settings = object : SettingsRepository {
                override fun loadSettings() = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light))
                override fun saveSettings(settings: AppSettings) {}
            }
            val workspace = object : TerminalWorkspaceRepository {
                override fun loadWorkspaceState() = TerminalWorkspaceState()
                override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
            }
            val sessions = InMemoryTerminalSessionManager()
            runDesktopComposeUiTest(width = 1100, height = 760) {
                setContent {
                    val capturedRoot = dataRoot
                    MoruApp(alpha.path, sessions, StaticRuntimeStatusRepository,
                        settings, workspace, repository, projectDataRoot = capturedRoot,
                        projectTreeEntries = entries,
                        onOpenProjectFile = { path ->
                            opened += capturedRoot to path
                            WorkbenchFilePreview.unavailable(path, "Test preview")
                        })
                }
                onNodeWithText("Code").performClick()
                onNodeWithText("alpha.txt").assertExists()
                onNodeWithContentDescription("Project Beta").performClick()
                onNodeWithText("Code").performClick()

                onNodeWithText("Loading project files…").assertExists()
                onNodeWithText("alpha.txt").assertDoesNotExist()
                runOnIdle {
                    assertTrue(opened.isEmpty())
                    dataRoot = beta.canonicalPath
                    entries = listOf(WorkbenchProjectTreeEntry("beta.txt", "beta.txt", 0, true))
                }
                onNodeWithText("beta.txt").performClick()
                runOnIdle { assertEquals(listOf(beta.canonicalPath to "beta.txt"), opened) }
            }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
