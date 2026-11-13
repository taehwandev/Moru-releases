package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.model.CodeExplorerOptions
import app.thdev.moru.core.model.files.WorkbenchProjectTreeEntry
import app.thdev.moru.core.model.git.WorkbenchGitStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The explorer's toolbar opens every readable folder, closes them, brings the open file into view and keeps options. */
@OptIn(ExperimentalTestApi::class)
class ProjectExplorerToolbarTest {
    private val entries = listOf(
        WorkbenchProjectTreeEntry("app", "app", depth = 0, isFile = false),
        WorkbenchProjectTreeEntry("src", "app/src", depth = 1, isFile = false),
        WorkbenchProjectTreeEntry("Main.kt", "app/src/Main.kt", depth = 2, isFile = true),
        WorkbenchProjectTreeEntry("build", "build", depth = 0, isFile = false, isIgnored = true),
        WorkbenchProjectTreeEntry("node_modules", "node_modules", depth = 0, isFile = false, isDeferred = true),
    )

    @Test
    fun expandCollapseLocateAndOptions() = runDesktopComposeUiTest {
        var expanded by mutableStateOf(emptySet<String>())
        var options by mutableStateOf(CodeExplorerOptions())
        setContent {
            MoruTheme {
                ProjectWorktreePane(
                    workspacePath = "/work/alpha",
                    entries = entries,
                    selectedFilePath = "app/src/Main.kt",
                    onOpenFile = {},
                    gitStatus = WorkbenchGitStatus.NotRepository,
                    onRefreshGitStatus = {},
                    expandedFolders = expanded,
                    onExpandedFoldersChange = { expanded = it },
                    options = options,
                    onOptionsChange = { options = it },
                    modifier = Modifier.width(320.dp).height(400.dp),
                )
            }
        }
        onNodeWithContentDescription("Expand all folders").performClick()
        runOnIdle { assertEquals(setOf("app", "app/src"), expanded, "ignored and unread folders stay closed") }
        onNodeWithContentDescription("Collapse all folders").performClick()
        runOnIdle { assertTrue(expanded.isEmpty()) }

        onNodeWithContentDescription("Show the open file in the tree").performClick()
        runOnIdle { assertEquals(setOf("app", "app/src"), expanded) }
        onNodeWithText("Main.kt").assertExists()

        val normal = onNodeWithText("Main.kt").fetchSemanticsNode().boundsInRoot.height
        onNodeWithContentDescription("Explorer options").performClick()
        onNodeWithText("Compact rows").performClick()
        runOnIdle { assertEquals(CodeExplorerOptions(compact = true), options) }
        val compactRow = onNodeWithText("Main.kt").fetchSemanticsNode().boundsInRoot.height
        assertTrue(compactRow <= normal, "compact rows are no taller")
    }
}
