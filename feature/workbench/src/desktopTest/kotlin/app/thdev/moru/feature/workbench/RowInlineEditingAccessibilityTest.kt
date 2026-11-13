package app.thdev.moru.feature.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.ui.identity.taskIdentityAccent
import app.thdev.moru.feature.workbench.screenshot.AttentionFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class RowInlineEditingAccessibilityTest {

    @Test
    fun projectRowExposesUnmergedControlsAndBlocksStrayRowSelectionDuringInlineRenameAndCloseConfirm() = runDesktopComposeUiTest {
        var selectedCount = 0
        var closedCount = 0
        val project = AttentionFixtures.sidebarState().projects.first().copy(rootPath = null)

        setContent {
            MoruTheme {
                Box(Modifier.width(260.dp)) {
                    ProjectRowView(
                        project = project,
                        onSelect = { selectedCount++ },
                        onToggle = {},
                        onClose = { closedCount++ },
                        runningSessions = 2,
                    )
                }
            }
        }

        // Open project menu and start inline rename
        onNodeWithContentDescription("Actions for ${project.displayName}").performClick()
        onNodeWithText("Rename").performClick()
        waitForIdle()

        // In the default merged accessibility tree (useUnmergedTree = false),
        // the inline text field is focused, and parent semantics is neither clickable nor disabled.
        onNodeWithTag("project-name", useUnmergedTree = false).assertExists().assertIsFocused()
        onNodeWithContentDescription("Cancel renaming", useUnmergedTree = false).assertExists().assertIsEnabled()
        val renamingParentSemantics = onNodeWithContentDescription("Project ${project.displayName}", useUnmergedTree = false)
            .fetchSemanticsNode()
        assertFalse(SemanticsActions.OnClick in renamingParentSemantics.config)
        assertFalse(SemanticsProperties.Disabled in renamingParentSemantics.config)

        // Clicking stray/warning text area inside the row while renaming does not select the project
        onNodeWithText("No folder linked", useUnmergedTree = false).performClick()
        runOnIdle { assertEquals(0, selectedCount) }

        // Cancel rename and trigger close confirmation
        onNodeWithContentDescription("Cancel renaming", useUnmergedTree = false).performClick()
        onNodeWithContentDescription("Actions for ${project.displayName}").performClick()
        onNodeWithText("Close ${project.displayName}").performClick()
        waitForIdle()

        // Confirmation controls are exposed in the merged accessibility tree and stray clicks do not select
        onNodeWithText("Stop and close", useUnmergedTree = false).assertExists().assertIsEnabled()
        onNodeWithText("Keep open", useUnmergedTree = false).assertExists().assertIsEnabled()
        val confirmParentSemantics = onNodeWithContentDescription("Project ${project.displayName}", useUnmergedTree = false)
            .fetchSemanticsNode()
        assertFalse(SemanticsActions.OnClick in confirmParentSemantics.config)
        assertFalse(SemanticsProperties.Disabled in confirmParentSemantics.config)

        onNodeWithText("2 agents are still running. Closing stops them.", useUnmergedTree = false).performClick()
        runOnIdle {
            assertEquals(0, selectedCount)
            assertEquals(0, closedCount)
        }
    }

    @Test
    fun taskRowExposesUnmergedControlsAndBlocksStrayRowSelectionDuringInlineRenameAndDeleteConfirm() = runDesktopComposeUiTest {
        var clickedCount = 0
        var deletedCount = 0
        val task = AttentionFixtures.sidebarState().projects.first().tasks.first()

        setContent {
            MoruTheme {
                Box(Modifier.width(260.dp)) {
                    TaskRowView(
                        task = task,
                        onClick = { clickedCount++ },
                        actions = TaskRowActions(onDelete = { deletedCount++ }),
                        accent = taskIdentityAccent(task.id),
                    )
                }
            }
        }

        // Open task menu and start inline rename
        onNodeWithContentDescription("Actions for ${task.title}").performClick()
        onNodeWithText("Rename").performClick()
        waitForIdle()

        // In the default merged accessibility tree, text field is focused and parent row is neither clickable nor disabled
        onNodeWithTag("task-name", useUnmergedTree = false).assertExists().assertIsFocused()
        onNodeWithContentDescription("Cancel renaming", useUnmergedTree = false).assertExists().assertIsEnabled()
        val renamingRowSemantics = onNodeWithContentDescription("Task ${task.title}", useUnmergedTree = false)
            .fetchSemanticsNode()
        assertFalse(SemanticsActions.OnClick in renamingRowSemantics.config)
        assertFalse(SemanticsProperties.Disabled in renamingRowSemantics.config)

        // Clicking secondary caption area while renaming does not select the task
        onNodeWithText("0 sessions", useUnmergedTree = false).performClick()
        runOnIdle { assertEquals(0, clickedCount) }

        // Cancel rename and trigger delete confirmation
        onNodeWithContentDescription("Cancel renaming", useUnmergedTree = false).performClick()
        onNodeWithContentDescription("Actions for ${task.title}").performClick()
        onNodeWithText("Delete").performClick()
        waitForIdle()

        // Delete confirmation controls are accessible in the merged tree and stray clicks do not select the task
        onNodeWithText("Delete task", useUnmergedTree = false).assertExists().assertIsEnabled()
        onNodeWithText("Keep", useUnmergedTree = false).assertExists().assertIsEnabled()
        val confirmRowSemantics = onNodeWithContentDescription("Task ${task.title}", useUnmergedTree = false)
            .fetchSemanticsNode()
        assertFalse(SemanticsActions.OnClick in confirmRowSemantics.config)
        assertFalse(SemanticsProperties.Disabled in confirmRowSemantics.config)

        onNodeWithText("0 sessions", useUnmergedTree = false).performClick()
        runOnIdle {
            assertEquals(0, clickedCount)
            assertEquals(0, deletedCount)
        }
    }
}
