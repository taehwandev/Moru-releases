package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.data.files.DesktopProjectFileRepository
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WorkbenchFolderDataTest {
    @Test
    fun switchingFoldersClearsPreviousTreeAndGitBeforeNewScanCompletes() = withFolders { alpha, beta ->
        var displayedRoot by mutableStateOf(alpha)
        val observed = AtomicReference<WorkbenchFolderData>()
        val betaScansStarted = CountDownLatch(2)
        val releaseBetaScans = CountDownLatch(1)
        fun waitForBeta(folder: File) {
            if (folder == beta) {
                betaScansStarted.countDown()
                check(releaseBetaScans.await(15, TimeUnit.SECONDS)) { "New folder scans were not released." }
            }
        }
        try {
            runDesktopComposeUiTest(width = 400, height = 300) {
                setContent {
                    observed.set(
                        rememberWorkbenchFolderData(
                            displayedRoot = displayedRoot,
                            hasProject = true,
                            treeRefreshKey = 0,
                            gitRefreshKey = 0,
                            scanTree = { folder -> waitForBeta(folder); files.tree(folder.absolutePath) },
                            scanGit = { folder ->
                                waitForBeta(folder)
                                WorkbenchGitStatus(true, folder.name, emptyList())
                            },
                        ),
                    )
                }
                waitUntil(timeoutMillis = 10_000) {
                    observed.get()?.let { data ->
                        data.treeEntries.any { it.path == "alpha.txt" } && data.gitStatus.branchName == "Alpha"
                    } == true
                }
                runOnIdle { displayedRoot = beta }
                waitUntil(timeoutMillis = 10_000) { betaScansStarted.count == 0L }
                runOnIdle {
                    val data = requireNotNull(observed.get())
                    assertEquals(beta.absolutePath, data.rootPath)
                    assertTrue(data.treeEntries.isEmpty())
                    assertEquals(WorkbenchGitStatus.NotRepository, data.gitStatus)
                }
                releaseBetaScans.countDown()
                waitUntil(timeoutMillis = 10_000) {
                    observed.get()?.let { data ->
                        data.treeEntries.any { it.path == "beta.txt" } && data.gitStatus.branchName == "Beta"
                    } == true
                }
                runOnIdle {
                    val data = requireNotNull(observed.get())
                    assertEquals(beta.absolutePath, data.rootPath)
                    assertTrue(data.treeEntries.none { it.path == "alpha.txt" })
                    assertEquals("Beta", data.gitStatus.branchName)
                }
            }
        } finally {
            releaseBetaScans.countDown()
        }
    }

    @Test
    fun observationsFromAnotherFolderCannotBeUsedWithCurrentFolderActions() = withFolders { alpha, beta ->
        val alphaData = WorkbenchFolderData(
            rootPath = alpha.absolutePath,
            treeEntries = files.tree(alpha.absolutePath),
            gitStatus = WorkbenchGitStatus(true, "Alpha", emptyList()),
        )

        val rejected = alphaData.forFolder(beta)

        assertTrue(alphaData.treeEntries.any { it.path == "alpha.txt" })
        assertEquals(beta.absolutePath, rejected.rootPath)
        assertTrue(rejected.treeEntries.isEmpty())
        assertEquals(WorkbenchGitStatus.NotRepository, rejected.gitStatus)
        assertEquals("Alpha content", files.open(alpha.absolutePath, "alpha.txt").content)
        assertEquals("Beta content", files.open(beta.absolutePath, "beta.txt").content)
    }

    private val files = DesktopProjectFileRepository()

    private fun withFolders(block: (File, File) -> Unit) {
        val root = Files.createTempDirectory("moru-folder-data").toFile()
        try {
            val alpha = root.resolve("Alpha").apply { mkdir() }
            val beta = root.resolve("Beta").apply { mkdir() }
            alpha.resolve("alpha.txt").writeText("Alpha content")
            beta.resolve("beta.txt").writeText("Beta content")
            block(alpha, beta)
        } finally {
            root.deleteRecursively()
        }
    }
}
