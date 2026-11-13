package app.thdev.moru.app

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import app.thdev.moru.core.data.*
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.model.*
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionKind
import app.thdev.moru.core.terminal.TerminalSessionSpec
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

/** Cmd+W asks in an alert over the window; once closed, Cmd+Shift+T brings the same terminal back. */
@OptIn(ExperimentalTestApi::class)
class CloseAndReopenTerminalInteractionTest {
    @Test
    fun closingAsksInAnAlertAndTheClosedTerminalOpensAgain() = withCatalog { catalog, root ->
        val project = catalog.registerProject("", root.path).project
        val task = catalog.createTask(project.id, "Example task", "Close and reopen")
        catalog.updateSelection(project.id, task.id)
        val navigation = WorkbenchNavigation(catalog)
        val sessions = InMemoryTerminalSessionManager()
        val claude = sessions.createSession(TerminalSessionSpec(title = "Claude", workingDirectory = root.canonicalPath,
            attachedItemId = task.id, startupCommand = "claude --session-id abc"))
        sessions.selectSession(claude.id)
        val commands = MoruCommands()
        val layout = WorkbenchLayoutKeeper(InMemoryWorkbenchLayoutRepository(), saveDelayMillis = 10)
        runDesktopComposeUiTest(width = 1300, height = 760) {
            setContent {
                MoruTheme(colors = lightCkColors) {
                    MoruWorkbenchHost(navigation, bindings(), rememberCoroutineScope(), root.path,
                        sessions, workspace(), AppSettings(), {},
                        commands = commands, layout = layout)
                }
            }
            waitUntil(timeoutMillis = 10_000) { commands.closeTerminal != null }
            assertNull(commands.reopenClosedTerminal, "Nothing was closed yet")

            // Cmd+W only asks, in an alert that names the terminal and says how to bring it back.
            runOnIdle { commands.closeTerminal!!.invoke() }
            onNodeWithTag("terminal-close-alert").assertExists()
            onNodeWithText("Close the Claude terminal?").assertExists()
            onNodeWithText("reopens it", substring = true).assertExists()
            assertEquals(1, sessions.listSessions().size)
            val capture = File("build/ui-captures/terminal-close-alert.png")
            capture.parentFile.mkdirs()
            ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", capture)

            // Escape keeps it.
            onNodeWithTag("terminal-close-alert").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("terminal-close-alert").assertDoesNotExist()
            assertEquals(1, sessions.listSessions().size)

            // Cmd+W twice closes it, and Cmd+Shift+T becomes available.
            runOnIdle { commands.closeTerminal!!.invoke() }
            runOnIdle { commands.closeTerminal!!.invoke() }
            onNodeWithTag("terminal-close-alert").assertDoesNotExist()
            waitUntil(timeoutMillis = 10_000) { sessions.listSessions().isEmpty() }
            waitUntil(timeoutMillis = 10_000) { commands.reopenClosedTerminal != null }

            runOnIdle { commands.reopenClosedTerminal!!.invoke() }
            runOnIdle {
                val reopened = sessions.listSessions().single()
                assertEquals("Claude", reopened.spec.title)
                assertEquals(task.id, reopened.spec.attachedItemId)
                assertEquals("claude --resume abc", reopened.spec.startupCommand)
                assertEquals(reopened.id, sessions.selectedSessionId())
                assertTrue(layout.layout.closedTerminals.isEmpty())
            }
            waitUntil(timeoutMillis = 10_000) { commands.reopenClosedTerminal == null }
        }
    }

    @Test
    fun theAlertsButtonClosesTheTerminal() = withCatalog { catalog, root ->
        val project = catalog.registerProject("", root.path).project
        val task = catalog.createTask(project.id, "Example task", "Close from the button")
        catalog.updateSelection(project.id, task.id)
        val sessions = InMemoryTerminalSessionManager()
        sessions.createSession(TerminalSessionSpec(title = "zsh", workingDirectory = root.canonicalPath, attachedItemId = task.id))
        val commands = MoruCommands()
        runDesktopComposeUiTest(width = 1300, height = 760) {
            setContent {
                MoruTheme(colors = lightCkColors) {
                    MoruWorkbenchHost(WorkbenchNavigation(catalog), bindings(), rememberCoroutineScope(), root.path,
                        sessions, workspace(), AppSettings(), {},
                        commands = commands, layout = WorkbenchLayoutKeeper(InMemoryWorkbenchLayoutRepository()))
                }
            }
            waitUntil(timeoutMillis = 10_000) { commands.closeTerminal != null }
            runOnIdle { commands.closeTerminal!!.invoke() }
            onNodeWithTag("terminal-close-confirm").performClick()
            waitUntil(timeoutMillis = 10_000) { sessions.listSessions().isEmpty() }
        }
    }

    private fun bindings() = MoruWorkbenchBindings(
        pickProjectFolder = { null }, selectProjectRoot = {}, viewFolderChanged = {},
        treeEntries = emptyList(), gitStatus = WorkbenchGitStatus.NotRepository, refreshGitStatus = {},
        loadGitHistory = { GitHistoryResult.Unavailable("No test history") },
        loadCommitDiff = { _, _ -> CommitDiffResult.Unavailable("No test diff") },
        loadWorkingChanges = { CommitDiffResult.Unavailable("No test diff") },
        openFile = { WorkbenchFilePreview.unavailable(it, "Not used") },
        saveFile = { path, _ -> WorkbenchFileSaveOutcome.Failed(path, "Not used") },
        createFile = { WorkbenchFileCreateOutcome.Failed(it, "Not used") },
        renameFile = { path, _ -> WorkbenchFileRenameOutcome.Failed(path, "Not used") },
        deleteFile = { WorkbenchFileDeleteOutcome.Failed(it, "Not used") }, refreshTree = {},
        dataRoot = null, startTerminal = { _, _, _ -> },
        // A test window has no native host for a real terminal.
        paneSlots = DesktopWorkbenchPaneSlots.copy(terminal = DesktopWorkbenchPaneSlots.terminal.copy(
            pane = { _, modifier -> MoruText("Test terminal view", modifier = modifier) })),
    )

    private fun workspace() = object : TerminalWorkspaceRepository {
        override fun loadWorkspaceState() = TerminalWorkspaceState()
        override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
    }

    private fun withCatalog(block: (RoomProjectCatalogRepository, File) -> Unit) {
        val directory = Files.createTempDirectory("moru-close-reopen-ui").toFile()
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
