package app.thdev.moru.app

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import app.thdev.moru.core.data.*
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.core.domain.*
import app.thdev.moru.core.model.*
import app.thdev.moru.core.model.git.*
import app.thdev.moru.core.model.files.WorkbenchFileCreateOutcome
import app.thdev.moru.core.model.files.WorkbenchFileDeleteOutcome
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFileRenameOutcome
import app.thdev.moru.core.model.files.WorkbenchFileSaveOutcome
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffHunk
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import app.thdev.moru.feature.workbench.*
import app.thdev.moru.core.model.git.history.GitHistoryResult
import app.thdev.moru.core.model.git.history.GitCommit
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ProjectGitInteractionTest {
    @Test
    fun projectGitOpensWithoutCreatingATaskOrTerminal() = withCatalog { catalog, root ->
        val project = catalog.registerProject("", root.path).project
        catalog.updateSelection(project.id, null)
        val sessions = InMemoryTerminalSessionManager()
        val folders = CopyOnWriteArrayList<String>()
        val git = recordingGit(folders)
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruApp(root.path, sessions, StaticRuntimeStatusRepository, settings(), workspace(), catalog,
                    gitRepository = git)
            }
            onNodeWithText("Git").performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-wip-row").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("git-root-path").assertTextEquals(root.canonicalPath)
            onNodeWithText("Branches").performClick()
            onNodeWithTag("git-branches").assertExists()
            onNodeWithText("Done").performClick()
            runOnIdle {
                assertEquals(setOf(root.canonicalPath), folders.toSet())
                assertTrue(sessions.listSessions().isEmpty())
                assertTrue(catalog.loadCatalog().projects.single().tasks.isEmpty())
            }
            val capture = File("build/ui-captures/project-git-no-task.png")
            capture.parentFile.mkdirs()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", capture)
        }
    }

    @Test
    fun gitReadsTheSelectedTasksExecutionFolderAndNeverTheProjectFallback() = withCatalog { catalog, root ->
        val project = catalog.registerProject("", root.path).project
        val first = catalog.createTask(project.id, "First task", "First request")
        val second = catalog.createTask(project.id, "Second task", "Second request")
        catalog.updateSelection(project.id, first.id)
        val taskFolders = mapOf(first.id to root.resolve("first-folder").apply { mkdir() }.canonicalPath,
            second.id to root.resolve("second-folder").apply { mkdir() }.canonicalPath)
        val folders = CopyOnWriteArrayList<String>()
        val git = recordingGit(folders)
        val navigation = WorkbenchNavigation(catalog).apply { tab = ProjectTab.Git }
        // Each Task's terminal runs in its own folder, which is the folder Git reads for it.
        val sessions = InMemoryTerminalSessionManager().also { manager ->
            taskFolders.forEach { (taskId, folder) ->
                manager.createSession(TerminalSessionSpec(title = "Claude", workingDirectory = folder, attachedItemId = taskId))
            }
        }
        val bindings = bindings(git)
        runDesktopComposeUiTest(width = 1100, height = 760) {
            setContent {
                MoruTheme(colors = lightCkColors) {
                    MoruWorkbenchHost(navigation, bindings, rememberCoroutineScope(), root.path,
                        sessions, workspace(), AppSettings(), {})
                }
            }
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-wip-row").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("git-root-path").assertTextEquals(taskFolders.getValue(first.id))
            runOnIdle {
                navigation.selectTask(project.id, second.id)
                navigation.tab = ProjectTab.Git
            }
            waitUntil(timeoutMillis = 10_000) { folders.contains(taskFolders.getValue(second.id)) }
            onNodeWithTag("git-root-path").assertTextEquals(taskFolders.getValue(second.id))
            runOnIdle {
                assertEquals(taskFolders.values.toSet(), folders.toSet())
                assertFalse(folders.contains(root.canonicalPath))
                assertEquals(2, sessions.listSessions().size)
            }
        }
    }

    @Test
    fun projectGitConnectsCurrentChangesAndCommitHistoryInOneSurface() = withCatalog { catalog, root ->
        val project = catalog.registerProject("", root.path).project
        catalog.updateSelection(project.id, null)
        val folders = CopyOnWriteArrayList<String>()
        val workingFolders = CopyOnWriteArrayList<String>()
        val commitFolders = CopyOnWriteArrayList<String>()
        val git = recordingGit(folders, listOf(GitWorkingFile("src/current.kt", ' ', 'M')))
        val navigation = WorkbenchNavigation(catalog)
        val bindings = bindings(git).copy(
            loadGitHistory = { GitHistoryResult.Loaded(listOf(
                GitCommit("abc123456", emptyList(), "Reviewer", "dev@example.com", 1, emptyList(), "Reviewed commit"),
            ), false) },
            loadWorkingChanges = { folder -> workingFolders += folder; patch("src/current.kt", "Uncommitted app change") },
            loadCommitDiff = { folder, hash ->
                assertEquals("abc123456", hash)
                commitFolders += folder
                patch("src/past.kt", "Committed app change")
            },
        )
        runDesktopComposeUiTest(width = 1400, height = 760) {
            setContent {
                MoruTheme(colors = lightCkColors) {
                    MoruWorkbenchHost(navigation, bindings, rememberCoroutineScope(), root.path,
                        InMemoryTerminalSessionManager(), workspace(), AppSettings(), {})
                }
            }
            runOnIdle { navigation.tab = ProjectTab.Git }
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-review-src/current.kt").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("git-review-src/current.kt").performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Uncommitted app change").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Uncommitted app change").assertIsDisplayed()
            onNodeWithTag("git-file-diff-close").performClick()
            // A commit lists its files beside the history; one of them opens its diff in the history's place.
            onNodeWithText("Reviewed commit", useUnmergedTree = true).performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("git-commit-file-src/past.kt").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("git-commit-file-src/past.kt").performClick()
            waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Committed app change").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Committed app change").assertIsDisplayed()
            onNodeWithTag("git-file-diff-close").performClick()
            onNodeWithTag("git-detail-close").performClick()
            onNodeWithTag("git-wip-row").performClick()
            onNodeWithTag("git-review-src/current.kt").assertExists()
            runOnIdle {
                assertEquals(setOf(root.canonicalPath), workingFolders.toSet())
                assertEquals(setOf(root.canonicalPath), commitFolders.toSet())
                assertTrue(catalog.loadCatalog().projects.single().tasks.isEmpty())
            }
        }
    }

    private fun patch(path: String, text: String) = CommitDiffResult.Loaded(listOf(
        DiffFile(path, path, DiffFileStatus.Modified, listOf(DiffHunk("@@ -1 +1 @@", listOf(
            DiffLine(DiffLineKind.Added, text, null, 1),
        ))), false),
    ), false)

    private fun recordingGit(folders: MutableList<String>, files: List<GitWorkingFile> = emptyList()) = object : GitRepository {
        override fun snapshot(workspacePath: String): GitRepositoryState {
            folders.add(workspacePath)
            return GitRepositoryState.Loaded(GitRepositorySnapshot(workspacePath, "main", "head",
                files, listOf(GitBranchInfo("main", isCurrent = true)), emptyList(), "reviewed"))
        }
        override fun execute(workspacePath: String, expectedRevision: String, action: GitAction): GitActionResult =
            error("Reading a Project or Task must not execute a Git action")
    }

    private fun bindings(git: GitRepository) = MoruWorkbenchBindings(
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
        gitRepository = git,
    )

    private fun settings() = object : SettingsRepository {
        override fun loadSettings() = AppSettings(appearance = AppearanceSettings(theme = AppTheme.Light))
        override fun saveSettings(settings: AppSettings) {}
    }

    private fun workspace() = object : TerminalWorkspaceRepository {
        override fun loadWorkspaceState() = TerminalWorkspaceState()
        override fun saveWorkspaceState(state: TerminalWorkspaceState) {}
    }

    private fun withCatalog(block: (RoomProjectCatalogRepository, File) -> Unit) {
        val directory = Files.createTempDirectory("moru-project-git-ui").toFile()
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
