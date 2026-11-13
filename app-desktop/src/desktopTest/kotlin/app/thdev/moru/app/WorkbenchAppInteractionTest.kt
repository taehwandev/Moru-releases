package app.thdev.moru.app

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

/** Runs the app host, so entry controls are checked against catalog and launch wiring. */
@OptIn(ExperimentalTestApi::class)
class WorkbenchAppInteractionTest {
    @Test
    fun folderPickerOpensProjectAndComposerAndATerminalThatCannotStartLeavesNoSession() = withCatalog { repository, root ->
        val sessions = InMemoryTerminalSessionManager()
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(
                    root.path, sessions, StaticRuntimeStatusRepository, testSettings(AppTheme.Light),
                    testWorkspace(), repository,
                    onPickProjectFolder = { root.path },
                    gitStatus = WorkbenchGitStatus(true, "main", emptyList()),
                    onStartTerminalSession = { _, _, _ -> error("Test terminal could not start") },
                )
            }
            onNodeWithText("A place for your next task").assertExists()
            onNodeWithText("History").assertDoesNotExist()
            onNodeWithText("Changes").assertDoesNotExist()
            onNodeWithText("Code").assertDoesNotExist()
            runOnIdle { assertTrue(sessions.listSessions().isEmpty()) }
            capture("empty-project")
            onNodeWithContentDescription("Switch project").performClick()
            onNodeWithText("Open project…").performClick()
            onNodeWithText("Describe what you want to change. Your task, terminal, and diff will stay together here.").assertExists()
            onNodeWithContentDescription("New task in", substring = true).performClick()
            capture("task-composer-light")
            onNodeWithText("Claude").performClick()
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Task could not start").fetchSemanticsNodes().isNotEmpty()
            }
            runOnIdle {
                assertTrue(sessions.listSessions().isEmpty())
                assertEquals(1, repository.loadCatalog().projects.single().tasks.size)
            }
            capture("task-launch-recovery")
        }
    }

    @Test
    fun narrowWindowKeepsProjectAndComposerOnTheSameRoot() = withCatalog { repository, root ->
        val sessions = InMemoryTerminalSessionManager()
        var viewedRoot: String? = null
        val registration = repository.registerProject("", root.path)
        repository.updateSelection(registration.project.id, null)
        runDesktopComposeUiTest(width = 800, height = 700) {
            setContent {
                MoruApp(
                    root.path, sessions, StaticRuntimeStatusRepository, testSettings(AppTheme.Dark),
                    testWorkspace(), repository,
                    onSelectProjectRoot = { viewedRoot = it },
                )
            }
            onNodeWithContentDescription("New task in", substring = true).performClick()
            onNodeWithText("Claude").assertIsDisplayed()
            capture("task-composer-narrow-dark")
            runOnIdle {
                assertEquals(root.canonicalPath, viewedRoot)
                assertTrue(sessions.listSessions().isEmpty())
            }
        }
    }

    @Test
    fun invalidStartupNoticeShowsRecoveryWithoutOpeningAnUnownedTerminal() = withCatalog { repository, root ->
        val sessions = InMemoryTerminalSessionManager()
        runDesktopComposeUiTest(width = 1000, height = 700) {
            setContent {
                MoruApp(root.path, sessions, StaticRuntimeStatusRepository, testSettings(AppTheme.Light),
                    testWorkspace(), repository, startupNotice = "The requested project folder does not exist.")
            }
            onNodeWithText("The requested project folder does not exist.").assertExists()
            onNodeWithText("A place for your next task").assertExists()
            runOnIdle { assertTrue(sessions.listSessions().isEmpty()) }
        }
    }

    @Test
    fun ordinaryFolderStartsInProjectRootEvenWhenGitReturnsADiagnostic() = withCatalog { repository, root ->
        val registration = repository.registerProject("", root.path)
        repository.updateSelection(registration.project.id, null)
        val sessions = InMemoryTerminalSessionManager()
        var attemptedFolder: String? = null
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(root.path, sessions, StaticRuntimeStatusRepository, testSettings(AppTheme.Light),
                    testWorkspace(), repository,
                    gitStatus = WorkbenchGitStatus.NotRepository.copy(message = "fatal: not a git repository"),
                    onStartTerminalSession = { session, _, _ ->
                        attemptedFolder = session.spec.workingDirectory
                        error("Test stops before starting an external runtime")
                    })
            }
            onNodeWithContentDescription("New task in", substring = true).performClick()
            onNodeWithText("Claude").performClick()
            waitUntil(timeoutMillis = 10_000) { attemptedFolder != null }
            runOnIdle {
                assertEquals(root.canonicalPath, attemptedFolder)
                assertTrue(sessions.listSessions().isEmpty())
            }
        }
    }

    @Test
    fun missingProjectHasNoHeaderNewTaskAndPreservesSelectedTask() = withCatalog { repository, root ->
        assertTrue(root.delete())
        val registration = repository.registerProject("", root.path)
        val task = repository.createTask(registration.project.id, "Existing task", "Keep this request")
        repository.updateSelection(registration.project.id, task.id)
        val sessions = InMemoryTerminalSessionManager()
        val settings = testSettings(AppTheme.Light)
        val workspace = testWorkspace()
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(root.path, sessions, StaticRuntimeStatusRepository, settings, workspace, repository)
            }
            // A new Task starts from the sidebar under its Project; the header no longer offers one.
            onAllNodesWithText("New task").assertCountEquals(0)
            runOnIdle { assertEquals(task.id, repository.loadCatalog().selectedTaskId) }
        }
    }

    @Test
    fun menuCommandsFollowTheOpenProjectAndCloseItWhenNothingIsRunning() = withCatalog { repository, root ->
        val sessions = InMemoryTerminalSessionManager()
        val commands = MoruCommands()
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(
                    root.path, sessions, StaticRuntimeStatusRepository, testSettings(AppTheme.Light),
                    testWorkspace(), repository,
                    onPickProjectFolder = { root.path },
                    commands = commands,
                )
            }
            // Without a project only the commands that do not need one are available.
            runOnIdle {
                assertNotNull(commands.openProject)
                assertNotNull(commands.showSettings)
                assertNull(commands.newTask)
                assertNull(commands.newSession)
                assertNull(commands.closeProject)
                assertNull(commands.showCode)
            }
            runOnIdle { commands.openProject!!() }
            waitUntil(timeoutMillis = 10_000) { commands.newTask != null }
            runOnIdle {
                assertNotNull(commands.closeProject)
                assertNotNull(commands.showWork)
                assertNotNull(commands.showCode)
                commands.newTask!!()
            }
            onNodeWithText("Claude").assertExists()
            runOnIdle { commands.closeProject!!() }
            waitUntil(timeoutMillis = 10_000) { commands.newTask == null }
            runOnIdle { assertTrue(repository.loadCatalog().projects.isEmpty()) }
        }
    }

    private fun ComposeUiTest.capture(name: String) {
        val output = File("build/ui-captures/$name.png")
        output.parentFile.mkdirs()
        // A composer popup adds another root; capture the owning app scene.
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", output)
    }

    private fun testSettings(theme: AppTheme) = object : SettingsRepository {
        private var value = AppSettings(appearance = AppearanceSettings(theme = theme))
        override fun loadSettings() = value
        override fun saveSettings(settings: AppSettings) { value = settings }
    }

    private fun testWorkspace() = object : TerminalWorkspaceRepository {
        override fun loadWorkspaceState() = TerminalWorkspaceState()
        override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
    }

    private fun withCatalog(block: (RoomProjectCatalogRepository, File) -> Unit) {
        val directory = Files.createTempDirectory("moru-app-ui").toFile()
        val root = directory.resolve("Example project").apply { mkdir() }
        val database = defaultDesktopMoruCatalogDatabase(directory.resolve("catalog"))
        try {
            block(RoomProjectCatalogRepository(database, DesktopProjectRootCanonicalizer(),
                { java.util.UUID.randomUUID().toString() }, System::currentTimeMillis), root)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
