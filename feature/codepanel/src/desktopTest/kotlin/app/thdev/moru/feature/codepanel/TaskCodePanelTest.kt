package app.thdev.moru.feature.codepanel

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.git.CodeWorktree
import app.thdev.moru.core.terminal.InMemoryTerminalSessionManager
import app.thdev.moru.core.terminal.TerminalSessionSpec
import app.thdev.moru.core.model.files.WorkbenchFilePreview
import app.thdev.moru.core.model.files.WorkbenchFilePreviewStatus
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.model.git.diff.DiffFile
import app.thdev.moru.core.model.git.diff.DiffFileStatus
import app.thdev.moru.core.model.git.diff.DiffHunk
import app.thdev.moru.core.model.git.diff.DiffLine
import app.thdev.moru.core.model.git.diff.DiffLineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TaskCodePanelTest {
    private val worktree = "/repo/.claude/worktrees/a"

    private val changedFile = DiffFile(
        oldPath = "src/App.kt",
        newPath = "src/App.kt",
        status = DiffFileStatus.Modified,
        hunks = listOf(
            DiffHunk(
                "@@ -1,2 +1,2 @@",
                listOf(
                    DiffLine(DiffLineKind.Context, "fun main() {", 1, 1),
                    DiffLine(DiffLineKind.Removed, "    println(\"old\")", 2, null),
                    DiffLine(DiffLineKind.Added, "    println(\"agent\")", null, 2),
                ),
            ),
        ),
        isBinary = false,
    )

    private inner class FakeSource : CodePanelSource {
        val readRoots = mutableListOf<String>()
        override fun worktrees(projectFolder: String) = listOf(
            CodeWorktree("/repo", "main", isMain = true),
            CodeWorktree(worktree, "feature-a", isMain = false),
        )
        override fun sessionWorktreeRoots(sessionId: String) = listOf("/repo", worktree)
        override fun changeStamp(worktreeRoot: String) = "stamp"
        override fun changes(worktreeRoot: String): CommitDiffResult {
            readRoots += worktreeRoot
            return CommitDiffResult.Loaded(listOf(changedFile), truncated = false)
        }
        override fun readFile(worktreeRoot: String, relativePath: String) = WorkbenchFilePreview(
            path = relativePath,
            name = relativePath.substringAfterLast('/'),
            status = WorkbenchFilePreviewStatus.TEXT,
            sizeBytes = 40,
            content = "fun main() {\n    println(\"agent\")\n}",
            summaryText = "",
        )
    }

    @Test
    fun followsTheTerminalIntoItsWorktreeAndShowsItsCode() = runDesktopComposeUiTest(width = 900, height = 600) {
        val sessions = InMemoryTerminalSessionManager(listOf(TerminalSessionSpec(attachedItemId = "task", workingDirectory = "/repo")))
        val source = FakeSource()
        val state = CodePanelState(emptyMap())
        setContent {
            MoruTheme {
                TaskCodePanel(source, state, "task", "/repo", sessions)
            }
        }

        waitUntil(timeoutMillis = 5_000) { source.readRoots.isNotEmpty() }
        waitForIdle()
        assertEquals(worktree, source.readRoots.last())
        onNodeWithContentDescription("Worktree: feature-a").assertExists()
        assertTrue(onAllNodesWithText("App.kt").fetchSemanticsNodes().isNotEmpty())
        onNodeWithText("    println(\"agent\")").assertExists()
    }

    @Test
    fun aLongFileShowsOnlyItsChangedRanges() = runDesktopComposeUiTest(width = 900, height = 600) {
        val lines = (1..60).map { n -> "val line$n = $n" }
        val long = DiffFile("src/Long.kt", "src/Long.kt", DiffFileStatus.Modified, listOf(
            DiffHunk("@@ -9,3 +9,3 @@", listOf(
                DiffLine(DiffLineKind.Context, lines[8], 9, 9),
                DiffLine(DiffLineKind.Removed, "val line10 = 0", 10, null),
                DiffLine(DiffLineKind.Added, lines[9], null, 10),
                DiffLine(DiffLineKind.Context, lines[10], 11, 11),
            )),
        ), isBinary = false)
        val base = FakeSource()
        val source = object : CodePanelSource by base {
            override fun worktrees(projectFolder: String) = listOf(CodeWorktree("/repo", "main", isMain = true))
            override fun sessionWorktreeRoots(sessionId: String) = listOf("/repo")
            override fun changes(worktreeRoot: String) = CommitDiffResult.Loaded(listOf(long), truncated = false)
            override fun readFile(worktreeRoot: String, relativePath: String) =
                base.readFile(worktreeRoot, relativePath).copy(content = lines.joinToString("\n"))
        }
        val sessions = InMemoryTerminalSessionManager(listOf(TerminalSessionSpec(attachedItemId = "task", workingDirectory = "/repo")))
        setContent { MoruTheme { TaskCodePanel(source, CodePanelState(emptyMap()), "task", "/repo", sessions) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("val line10 = 10").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("8 unchanged lines").assertExists()
        onNodeWithText("49 unchanged lines").assertExists()
        assertTrue(onAllNodesWithText("val line1 = 1").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithText("val line30 = 30").fetchSemanticsNodes().isEmpty())
        val capture = java.io.File("build/ui-captures/code-panel-changed-ranges.png")
        capture.parentFile.mkdirs()
        javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", capture)
    }
}
